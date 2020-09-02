
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
 *         &lt;element name="ECHOReturn" type="{http://www.w3.org/2001/XMLSchema}string"/>
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
    "echoReturn"
})
@XmlRootElement(name = "ECHOResponse")
public class ECHOResponse {

    @XmlElement(name = "ECHOReturn", required = true)
    protected String echoReturn;

    /**
     * Gets the value of the echoReturn property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getECHOReturn() {
        return echoReturn;
    }

    /**
     * Sets the value of the echoReturn property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setECHOReturn(String value) {
        this.echoReturn = value;
    }

}
