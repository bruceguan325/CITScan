package com.intumit.solr.util;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.AudioRequest;
import com.google.cloud.speech.v1.InitialRecognizeRequest;
import com.google.cloud.speech.v1.InitialRecognizeRequest.AudioEncoding;
import com.google.cloud.speech.v1.RecognizeRequest;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechGrpc;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.auth.ClientAuthInterceptor;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client that sends streaming audio to Speech.Recognize and returns streaming
 * transcript.
 */
public class GoogleSpeechToText {

	private final String host;
	private final int port;
	private final InputStream file;
	private final int samplingRate;

	private static final Logger logger = Logger.getLogger(GoogleSpeechToText.class
			.getName());

	private final ManagedChannel channel;

	private final SpeechGrpc.SpeechStub stub;

	public String parseText = "";
	public double parseValue = 0.0;

	private static final List<String> OAUTH2_SCOPES = Arrays
			.asList("https://www.googleapis.com/auth/cloud-platform");

	/**
	 * Construct client connecting to Cloud Speech server at {@code host:port}.
	 */
	public GoogleSpeechToText(String host, int port, InputStream file, int samplingRate)
			throws IOException {
		this.host = host;
		this.port = port;
		this.file = file;
		this.samplingRate = samplingRate;

		GoogleCredentials creds = GoogleCredentials.getApplicationDefault();
		creds = creds.createScoped(OAUTH2_SCOPES);
		channel = NettyChannelBuilder
				.forAddress(host, port)
				.negotiationType(NegotiationType.TLS)
				.intercept(
						new ClientAuthInterceptor(creds, Executors
								.newSingleThreadExecutor())).build();
		stub = SpeechGrpc.newStub(channel);
		logger.info("Created stub for " + host + ":" + port);
	}

	public void shutdown() throws InterruptedException {
		channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
	}

	/** Send streaming recognize requests to server. */
	public void recognize() throws InterruptedException, IOException {
		final CountDownLatch finishLatch = new CountDownLatch(1);
		StreamObserver<RecognizeResponse> responseObserver = new StreamObserver<RecognizeResponse>() {
			@Override
			public void onNext(RecognizeResponse response) {
				if (response.getResultsCount() > 0) {				
					if (response.getResults(0).getIsFinal()) {
						if (response.getResults(0).getAlternativesCount() > 0) {
							SpeechRecognitionAlternative a = response.getResults(0).getAlternatives(0);
							setParseText(a.getTranscript());
							setParseValue(a.getConfidence());
						}
					}else if (response.getResults(0).getAlternativesCount() > 0) {
						SpeechRecognitionAlternative a = response.getResults(0).getAlternatives(0);
						setParseText(a.getTranscript());
					}
					logger.info("Received response: " + TextFormat.printToUnicodeString(response));
				}
			}

			@Override
			public void onError(Throwable error) {
				Status status = Status.fromThrowable(error);
				logger.log(Level.WARNING, "recognize failed: {0}", status);
				finishLatch.countDown();
			}

			@Override
			public void onCompleted() {
				logger.info("recognize completed.");
				finishLatch.countDown();
			}
		};

		StreamObserver<RecognizeRequest> requestObserver = stub
				.recognize(responseObserver);
		try {
			// Build and send a RecognizeRequest containing the parameters for
			// processing the audio.
			InitialRecognizeRequest initial = InitialRecognizeRequest
					.newBuilder().setEncoding(AudioEncoding.LINEAR16)
					.setSampleRate(samplingRate).setInterimResults(true)
					.setLanguageCode("zh-TW").build();
			RecognizeRequest firstRequest = RecognizeRequest.newBuilder()
					.setInitialRequest(initial).build();
			requestObserver.onNext(firstRequest);

			// Open audio file. Read and send sequential buffers of audio as
			// additional RecognizeRequests.
			//FileInputStream in = new FileInputStream(new File(file));
			// For LINEAR16 at 16000 Hz sample rate, 3200 bytes corresponds to
			// 100 milliseconds of audio.
			byte[] buffer = new byte[3200];
			int bytesRead;
			int totalBytes = 0;
			while ((bytesRead = file.read(buffer)) != -1) {
				totalBytes += bytesRead;
				AudioRequest audio = AudioRequest.newBuilder()
						.setContent(ByteString.copyFrom(buffer, 0, bytesRead))
						.build();
				RecognizeRequest request = RecognizeRequest.newBuilder()
						.setAudioRequest(audio).build();
				requestObserver.onNext(request);
				// To simulate real-time audio, sleep after sending each audio
				// buffer.
				// For 16000 Hz sample rate, sleep 100 milliseconds.
				Thread.sleep(samplingRate / 160);
			}
			logger.info("Sent " + totalBytes + " bytes from audio file: "
					+ file);
		} catch (RuntimeException e) {
			// Cancel RPC.
			requestObserver.onError(e);
			throw e;
		}
		// Mark the end of requests.
		requestObserver.onCompleted();

		// Receiving happens asynchronously.
		finishLatch.await(1, TimeUnit.MINUTES);
	}

	public String getParseText() {
		return parseText;
	}

	public void setParseText(String parseText) {
		this.parseText = parseText;
	}

	public double getParseValue() {
		return parseValue;
	}

	public void setParseValue(double parseValue) {
		this.parseValue = parseValue;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

}
