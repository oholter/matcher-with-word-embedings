package mappings.utils;

import java.util.Optional;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleIRI;

import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class Rdf4j2Jena {

	public static com.hp.hpl.jena.rdf.model.Statement convert(com.hp.hpl.jena.rdf.model.Model model,
			org.eclipse.rdf4j.model.Statement stmt) {
		Resource resource = rdf4jResourceToJenaResource(model, stmt.getSubject());
		Property property = rdf4jPropertyToJenaProperty(model, (SimpleIRI) stmt.getPredicate());
		RDFNode node = rdf4jValueToJenaRdfNode(model, stmt.getObject());

		com.hp.hpl.jena.rdf.model.Statement jenaStatement = ResourceFactory.createStatement(resource, property, node);

		return jenaStatement;
	}

	private static Resource rdf4jResourceToJenaResource(Model jenaModel, org.eclipse.rdf4j.model.Resource resource) {
		if (resource instanceof SimpleIRI) {
			return jenaModel.createResource(resource.stringValue());
		} else {
			return jenaModel.createResource(new AnonId(resource.stringValue()));
		}
	}

	private static Property rdf4jPropertyToJenaProperty(Model jenaModel, SimpleIRI resource) {
		return jenaModel.createProperty(resource.stringValue());
	}

	private static RDFNode rdf4jValueToJenaRdfNode(Model jenaModel, Value value) {
		if (value instanceof org.eclipse.rdf4j.model.Resource) {
			return rdf4jResourceToJenaResource(jenaModel, (org.eclipse.rdf4j.model.Resource) value);
		} else {
			return rdf4jLiteralToJenaRdfNode(jenaModel, (org.eclipse.rdf4j.model.Literal) value);
		}
	}

	private static RDFNode rdf4jLiteralToJenaRdfNode(Model jenaModel, org.eclipse.rdf4j.model.Literal value) {
		final Optional<String> language = value.getLanguage();
		if (value.getDatatype() != null) {
			return jenaModel.createTypedLiteral(value.stringValue(), value.getDatatype().stringValue());
		} else if (language.isPresent()) {
			return jenaModel.createLiteral(value.stringValue(), language.get());
		} else {
			return jenaModel.createLiteral(value.stringValue());
		}
	}
}
