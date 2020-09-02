
package tw.com.cathaybk.webservice.client.bancs;

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
 *         &lt;element name="RequstXML" type="{http://www.w3.org/2001/XMLSchema}string"/>
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
    "requstXML"
})
@XmlRootElement(name = "XServiceMethod")
public class XServiceMethod {

    @XmlElement(name = "RequstXML", required = true)
    protected String requstXML;

    /**
     * Gets the value of the requstXML property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRequstXML() {
        return requstXML;
    }

    /**
     * Sets the value of the requstXML property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRequstXML(String value) {
        this.requstXML = value;
    }

}
