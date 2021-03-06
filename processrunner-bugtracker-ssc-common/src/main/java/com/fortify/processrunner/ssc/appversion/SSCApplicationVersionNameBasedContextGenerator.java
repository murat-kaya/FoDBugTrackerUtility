/*******************************************************************************
 * (c) Copyright 2017 EntIT Software LLC, a Micro Focus company
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.processrunner.ssc.appversion;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import com.fortify.client.ssc.api.query.builder.SSCApplicationVersionsQueryBuilder;
import com.fortify.processrunner.context.Context;
import com.fortify.processrunner.ssc.appversion.json.preprocessor.filter.SSCJSONMapFilterListenerLoggerApplicationVersion;
import com.fortify.util.rest.json.JSONMap;
import com.fortify.util.rest.json.preprocessor.filter.AbstractJSONMapFilter;
import com.fortify.util.rest.json.preprocessor.filter.AbstractJSONMapFilter.MatchMode;
import com.fortify.util.rest.json.preprocessor.filter.JSONMapFilterListenerLogger.LogLevel;
import com.fortify.util.spring.SpringExpressionUtil;

/**
 * Filter SSC application versions based on application and version name,
 * and add corresponding context properties.
 * 
 * @author Ruud Senden
 *
 */
public class SSCApplicationVersionNameBasedContextGenerator extends AbstractSSCApplicationVersionContextGenerator {
	private LinkedHashMap<Pattern, Context> applicationVersionNamePatternToContextMap = null;
	
	@Override
	protected void updateApplicationVersionsQueryBuilderForSearch(Context initialContext, SSCApplicationVersionsQueryBuilder builder) {
		builder.preProcessor(new SSCJSONMapFilterApplicationVersionNamePatterns(MatchMode.INCLUDE, applicationVersionNamePatternToContextMap.keySet()));
	}
	
	@Override
	public void updateContextForApplicationVersion(Context context, JSONMap applicationVersion) {
		addMappedAttributes(context, applicationVersion, getContextForApplicationVersion(applicationVersion));
	}

	/**
	 * Configure a mapping between regular expressions that match
	 * [applicationName]:[versionName], and corresponding context 
	 * properties. Context property values may contain Spring Template 
	 * Expressions referencing the application version JSON attributes.
	 * @param applicationVersionNameMappings
	 */
	public void setApplicationVersionNameMappings(LinkedHashMap<Pattern, Context> applicationVersionNameMappings) {
		this.applicationVersionNamePatternToContextMap = applicationVersionNameMappings;
	}
	
	private Context getContextForApplicationVersion(JSONMap applicationVersion) {
		String name = SpringExpressionUtil.evaluateExpression(applicationVersion, "project.name+':'+name", String.class);
		for ( Map.Entry<Pattern, Context> entry : applicationVersionNamePatternToContextMap.entrySet() ) {
			if ( entry.getKey().matcher(name).matches() ) { return entry.getValue(); }
		}
		return null;
	}
	
	private void addMappedAttributes(Context initialContext, JSONMap applicationVersion, Context mappedContext) {
		if ( mappedContext != null ) {
			for ( Entry<String, Object> entry : mappedContext.entrySet() ) {
				initialContext.put(entry.getKey(), SpringExpressionUtil.evaluateTemplateExpression(applicationVersion, (String)entry.getValue(), Object.class));
			}
		}
	}
	
	private static final class SSCJSONMapFilterApplicationVersionNamePatterns extends AbstractJSONMapFilter {
		private final Set<Pattern> applicationVersionNamePatterns;
		public SSCJSONMapFilterApplicationVersionNamePatterns(MatchMode matchMode, Set<Pattern> applicationVersionNamePatterns) {
			super(matchMode);
			this.applicationVersionNamePatterns = applicationVersionNamePatterns;
			addFilterListeners(new SSCJSONMapFilterListenerLoggerApplicationVersion(LogLevel.INFO, 
					null,
					"name ${textObjectDoesOrDoesnt} match any RegEx "+applicationVersionNamePatterns));
		}
		
		@Override
		protected boolean isMatching(JSONMap applicationVersion) {
			String name = SpringExpressionUtil.evaluateExpression(applicationVersion, "project.name+':'+name", String.class);
			for ( Pattern pattern : applicationVersionNamePatterns ) {
				if ( pattern.matcher(name).matches() ) { return true; }
			}
			return false;
		}
		
	}
}
