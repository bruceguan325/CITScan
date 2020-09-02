
package tw.com.cathaybk.webservice.client.card;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.4-b01
 * Generated source version: 2.2
 * 
 */
@WebServiceClient(name = "CARDServiceService", targetNamespace = "http://webservice.cathaybk.com.tw", wsdlLocation = "file:/Users/yao/git/SmartSRM_Robot/wsdl/CARDService.xml")
public class CARDServiceService
    extends Service
{

    private final static URL CARDSERVICESERVICE_WSDL_LOCATION;
    private final static WebServiceException CARDSERVICESERVICE_EXCEPTION;
    private final static QName CARDSERVICESERVICE_QNAME = new QName("http://webservice.cathaybk.com.tw", "CARDServiceService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("file:/Users/yao/git/SmartSRM_Robot/wsdl/CARDService.xml");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        CARDSERVICESERVICE_WSDL_LOCATION = url;
        CARDSERVICESERVICE_EXCEPTION = e;
    }

    public CARDServiceService() {
        super(__getWsdlLocation(), CARDSERVICESERVICE_QNAME);
    }

    public CARDServiceService(WebServiceFeature... features) {
        super(__getWsdlLocation(), CARDSERVICESERVICE_QNAME, features);
    }

    public CARDServiceService(URL wsdlLocation) {
        super(wsdlLocation, CARDSERVICESERVICE_QNAME);
    }

    public CARDServiceService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, CARDSERVICESERVICE_QNAME, features);
    }

    public CARDServiceService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public CARDServiceService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *     returns CARDService
     */
    @WebEndpoint(name = "CARDService")
    public CARDService getCARDService() {
        return super.getPort(new QName("http://webservice.cathaybk.com.tw", "CARDService"), CARDService.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns CARDService
     */
    @WebEndpoint(name = "CARDService")
    public CARDService getCARDService(WebServiceFeature... features) {
        return super.getPort(new QName("http://webservice.cathaybk.com.tw", "CARDService"), CARDService.class, features);
    }

    private static URL __getWsdlLocation() {
        if (CARDSERVICESERVICE_EXCEPTION!= null) {
            throw CARDSERVICESERVICE_EXCEPTION;
        }
        return CARDSERVICESERVICE_WSDL_LOCATION;
    }

}