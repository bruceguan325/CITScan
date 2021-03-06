
package tw.com.cathaybk.webservice.client.fep;

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
@WebServiceClient(name = "FEPServiceService", targetNamespace = "http://webservice.cathaybk.com.tw", wsdlLocation = "file:/home/yao/git/SmartSRM_Robot/wsdl/FEPService.xml")
public class FEPServiceService
    extends Service
{

    private final static URL FEPSERVICESERVICE_WSDL_LOCATION;
    private final static WebServiceException FEPSERVICESERVICE_EXCEPTION;
    private final static QName FEPSERVICESERVICE_QNAME = new QName("http://webservice.cathaybk.com.tw", "FEPServiceService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("file:/home/yao/git/SmartSRM_Robot/wsdl/FEPService.xml");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        FEPSERVICESERVICE_WSDL_LOCATION = url;
        FEPSERVICESERVICE_EXCEPTION = e;
    }

    public FEPServiceService() {
        super(__getWsdlLocation(), FEPSERVICESERVICE_QNAME);
    }

    public FEPServiceService(WebServiceFeature... features) {
        super(__getWsdlLocation(), FEPSERVICESERVICE_QNAME, features);
    }

    public FEPServiceService(URL wsdlLocation) {
        super(wsdlLocation, FEPSERVICESERVICE_QNAME);
    }

    public FEPServiceService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, FEPSERVICESERVICE_QNAME, features);
    }

    public FEPServiceService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public FEPServiceService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *     returns FEPService
     */
    @WebEndpoint(name = "FEPService")
    public FEPService getFEPService() {
        return super.getPort(new QName("http://webservice.cathaybk.com.tw", "FEPService"), FEPService.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns FEPService
     */
    @WebEndpoint(name = "FEPService")
    public FEPService getFEPService(WebServiceFeature... features) {
        return super.getPort(new QName("http://webservice.cathaybk.com.tw", "FEPService"), FEPService.class, features);
    }

    private static URL __getWsdlLocation() {
        if (FEPSERVICESERVICE_EXCEPTION!= null) {
            throw FEPSERVICESERVICE_EXCEPTION;
        }
        return FEPSERVICESERVICE_WSDL_LOCATION;
    }

}
