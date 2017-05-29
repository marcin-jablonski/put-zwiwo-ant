package org.ant.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.FileResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

public class Meta4Task extends Task {
	private FileSet _fileset;

	private String _url;

	private String _file;

	private Document xmlDoc;

	public void addFileset(FileSet fileset){
		_fileset = fileset;
	}

	public void setUrl(String url) {_url = url;}

	public void setFile(String file) {_file = file;}

	@Override
	public void setProject(Project project) {
		super.setProject(project);
	}

	@Override
	public void execute() throws BuildException {

		if (_url == null) {
			_url = this.getProject().getProperty("server.files.url");
		}

        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            xmlDoc = builder.newDocument();
            Element root = xmlDoc.createElementNS("urn:ietf:params:xml:ns:metalink", "metalink");
            xmlDoc.appendChild(root);

            Iterator it = _fileset.iterator();

            while(it.hasNext()) {
                FileResource file = (FileResource) it.next();
                if(!file.isDirectory()) {
                    root.appendChild(createFileXmlElement(file));
                }
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(xmlDoc);
            StreamResult result = new StreamResult(new FileOutputStream(_file));
            transformer.transform(source, result);
        } catch (ParserConfigurationException | IOException | TransformerException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


    }

	private Element createFileXmlElement(FileResource file) throws IOException, NoSuchAlgorithmException {
        Element fileXmlElement = xmlDoc.createElement("file");
        fileXmlElement.setAttribute("name", file.getFile().getName());
        fileXmlElement.appendChild(createFilePropertyXmlElement("size", Long.toString(file.getFile().length())));
        Element hashProperty = createFilePropertyXmlElement("hash", generateChecksumForFile(file.getFile()));
        hashProperty.setAttribute("type", "md5");
        fileXmlElement.appendChild(hashProperty);
        fileXmlElement.appendChild(createFilePropertyXmlElement("url", _url + file.getFile().getAbsolutePath().replace(_fileset.getDir().getAbsolutePath() + File.separator, "").replace('\\', '/')));
        return fileXmlElement;
    }

    private Element createFilePropertyXmlElement(String name, String value) {
        Element propertyXmlElement = xmlDoc.createElement(name);
        propertyXmlElement.appendChild(xmlDoc.createTextNode(value));
        return propertyXmlElement;
    }

    private String generateChecksumForFile(File file) throws NoSuchAlgorithmException, IOException {
        byte[] digest;
        digest = MessageDigest.getInstance("MD5").digest(Files.readAllBytes(file.toPath()));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
