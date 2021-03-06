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
package com.fortify.processrunner.ssc.connection;

import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.processrunner.context.Context;
import com.fortify.processrunner.context.ContextPropertyDefinition;
import com.fortify.processrunner.context.ContextPropertyDefinitions;
import com.fortify.processrunner.ssc.context.IContextSSCCommon;
import com.fortify.processrunner.util.rest.ContextAwareProxyConfigurationFactory;
import com.fortify.util.rest.connection.ProxyConfig;

/**
 * This class allows for instantiating and caching {@link SSCAuthenticatingRestConnection}
 * instances based on {@link Context} properties.
 * 
 * @author Ruud Senden
 *
 */
public final class SSCConnectionFactory 
{
	public static final void addContextPropertyDefinitions(ContextPropertyDefinitions contextPropertyDefinitions, Context context) {
		contextPropertyDefinitions.add(new ContextPropertyDefinition(IContextSSCCommon.PRP_SSC_BASE_URL, "SSC base URL", true).readFromConsole(true));
		contextPropertyDefinitions.add(new ContextPropertyDefinition(IContextSSCCommon.PRP_SSC_USER_NAME, "SSC user name (leave blank to use auth token)", true).readFromConsole(true).isAlternativeForProperties(IContextSSCCommon.PRP_SSC_AUTH_TOKEN));
		contextPropertyDefinitions.add(new ContextPropertyDefinition(IContextSSCCommon.PRP_SSC_PASSWORD, "SSC password", true).readFromConsole(true).isPassword(true).dependsOnProperties(IContextSSCCommon.PRP_SSC_USER_NAME));
		contextPropertyDefinitions.add(new ContextPropertyDefinition(IContextSSCCommon.PRP_SSC_AUTH_TOKEN, "SSC auth token (leave blank to use username/password)", true).readFromConsole(true).isPassword(true).isAlternativeForProperties(IContextSSCCommon.PRP_SSC_USER_NAME));
		ContextAwareProxyConfigurationFactory.addContextPropertyDefinitions(contextPropertyDefinitions, context, "SSC");
	}
	
	public static final SSCAuthenticatingRestConnection getConnection(Context context) {
		IContextSSCConnection ctx = context.as(IContextSSCConnection.class);
		SSCAuthenticatingRestConnection result = ctx.getSSCConnection();
		if ( result == null ) {
			result = createConnection(context);
			ctx.setSSCConnection(result);
		}
		return result;
	}

	private static final SSCAuthenticatingRestConnection createConnection(Context context) {
		IContextSSCCommon ctx = context.as(IContextSSCCommon.class);
		
		ProxyConfig proxy = ContextAwareProxyConfigurationFactory.getProxyConfiguration(context, "SSC");
		return SSCAuthenticatingRestConnection.builder()
			.proxy(proxy)
			.enableSerializationSingleJVM()
			.baseUrl(ctx.getSSCBaseUrl())
			.authToken(ctx.getSSCAuthToken())
			.userName(ctx.getSSCUserName())
			.password(ctx.getSSCPassword())
			.build();
	}
	
	private interface IContextSSCConnection {
		public void setSSCConnection(SSCAuthenticatingRestConnection connection);
		public SSCAuthenticatingRestConnection getSSCConnection();
	}
}
