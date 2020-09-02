
package tw.com.cathaybk.webservice.client.fep;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.4-b01
 * Generated source version: 2.2
 * 
 */
@WebService(name = "FEPService", targetNamespace = "http://webservice.cathaybk.com.tw")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface FEPService {


    /**
     * 
     * @param requstXML
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "XServiceMethod")
    @WebResult(name = "XServiceMethodReturn", targetNamespace = "http://webservice.cathaybk.com.tw")
    @RequestWrapper(localName = "XServiceMethod", targetNamespace = "http://webservice.cathaybk.com.tw", className = "tw.com.cathaybk.webservice.client.fep.XServiceMethod")
    @ResponseWrapper(localName = "XServiceMethodResponse", targetNamespace = "http://webservice.cathaybk.com.tw", className = "tw.com.cathaybk.webservice.client.fep.XServiceMethodResponse")
    public String xServiceMethod(
        @WebParam(name = "RequstXML", targetNamespace = "http://webservice.cathaybk.com.tw")
        String requstXML);

    /**
     * 
     * @param requestString
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "ECHO")
    @WebResult(name = "ECHOReturn", targetNamespace = "http://webservice.cathaybk.com.tw")
    @RequestWrapper(localName = "ECHO", targetNamespace = "http://webservice.cathaybk.com.tw", className = "tw.com.cathaybk.webservice.client.fep.ECHO")
    @ResponseWrapper(localName = "ECHOResponse", targetNamespace = "http://webservice.cathaybk.com.tw", className = "tw.com.cathaybk.webservice.client.fep.ECHOResponse")
    public String echo(
        @WebParam(name = "RequestString", targetNamespace = "http://webservice.cathaybk.com.tw")
        String requestString);

}
