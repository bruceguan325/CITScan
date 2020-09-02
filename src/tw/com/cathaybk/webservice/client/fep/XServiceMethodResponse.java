
package tw.com.cathaybk.webservice.client.fep;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="XServiceMethodReturn" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "xServiceMethodReturn"
})
@XmlRootElement(name = "XServiceMethodResponse")
public class XServiceMethodResponse {

    @XmlElement(name = "XServiceMethodReturn", required = true)
    protected String xServiceMethodReturn;

    /**
     * Gets the value of the xServiceMethodReturn property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getXServiceMethodReturn() {
        return xServiceMethodReturn;
    }

    /**
     * Sets the value of the xServiceMethodReturn property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setXServiceMethodReturn(String value) {
        this.xServiceMethodReturn = value;
    }

}
