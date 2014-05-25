package eu.ldbc.semanticpublishing.templates.aggregation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionParametersGenerator;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query6.txt
 */
public class Query6Template extends MustacheTemplate implements SubstitutionParametersGenerator {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query6.txt";
	
	private final RandomUtil ru;
	
	public Query6Template(RandomUtil ru, HashMap<String, String> queryTemplates, Definitions definitions, String[] substitutionParameters) {
		super(queryTemplates, substitutionParameters);
		this.ru = ru;
	}
	
	/**
	 * A method for replacing mustache template : {{{cwLiveCoverage}}}
	 */	
	public String cwLiveCoverage() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}		
		
		return ru.createBoolean(ru.nextBoolean());
	}
	
	/**
	 * A method for replacing mustache template : {{{cwAudience}}}
	 */	
	public String cwAudience() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		if (ru.nextBoolean()) {
			return "cwork:NationalAudience";
		} else {
			return "cwork:InternationalAudience";
		}
	}
	
	/**
	 * A method for replacing mustache template : {{{randomLimit}}}
	 */	
	public String randomLimit() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}		
		
		return "10";
	}	
	
	@Override
	public String generateSubstitutionParameters(BufferedWriter bw, int amount) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < amount; i++) {
			sb.setLength(0);
			sb.append(cwLiveCoverage());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwAudience());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(randomLimit());
			sb.append("\n");
			bw.write(sb.toString());
		}
		return null;
	}	
	
	@Override
	public String getTemplateFileName() {
		return templateFileName;
	}

	@Override
	public QueryType getTemplateQueryType() {
		return QueryType.SELECT;
	}	
}
