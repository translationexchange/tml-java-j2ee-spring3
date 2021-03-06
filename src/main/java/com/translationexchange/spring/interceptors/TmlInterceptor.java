/**
 * Copyright (c) 2016 Translation Exchange, Inc. All rights reserved.
 *
 *  _______                  _       _   _             ______          _
 * |__   __|                | |     | | (_)           |  ____|        | |
 *    | |_ __ __ _ _ __  ___| | __ _| |_ _  ___  _ __ | |__  __  _____| |__   __ _ _ __   __ _  ___
 *    | | '__/ _` | '_ \/ __| |/ _` | __| |/ _ \| '_ \|  __| \ \/ / __| '_ \ / _` | '_ \ / _` |/ _ \
 *    | | | | (_| | | | \__ \ | (_| | |_| | (_) | | | | |____ >  < (__| | | | (_| | | | | (_| |  __/
 *    |_|_|  \__,_|_| |_|___/_|\__,_|\__|_|\___/|_| |_|______/_/\_\___|_| |_|\__,_|_| |_|\__, |\___|
 *                                                                                        __/ |
 *                                                                                       |___/
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.translationexchange.spring.interceptors;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.translationexchange.core.Session;
import com.translationexchange.core.Tml;
import com.translationexchange.core.Utils;
import com.translationexchange.j2ee.utils.SecurityUtils;
import com.translationexchange.j2ee.utils.SessionUtils;
 
public class TmlInterceptor implements HandlerInterceptor  {
	public static final String TML_SESSION_KEY = "tml";
	private Long t0;

	/**
	 * Returns current locale - must be overwritten
	 * 
	 * @return
	 */
	protected String getCurrentLocale(HttpServletRequest req) {
    	return null;
    }

	/**
	 * Returns current source - must be overwritten
	 * 
	 * @return
	 */
    protected String getCurrentSource(HttpServletRequest req) {
    	return null;
    }
    
    /**
     * Prepares initialization parameters
     *  
     * @param req
     * @param resp
     * @return
     * @throws ServletException
     * @throws IOException
     */
	protected Map<String, Object> prepareSessionParams(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	Map<String, Object> params = SecurityUtils.decode(SessionUtils.getSessionCookie(SessionUtils.getCookieName(), req));
    	if (params != null) Tml.getLogger().debug(params);
    	
    	// Locale can be forced by the user
    	String locale = getCurrentLocale(req);
	    if (locale == null) {
	    	// Or passed as a parameter
	    	locale = req.getParameter("locale");
	    	if (locale != null) {
	    		params.put("locale", locale);
	    		SessionUtils.setSessionCookie(SessionUtils.getCookieName(), SecurityUtils.encode(params), resp);
			    Tml.getLogger().debug("Param Locale: " + locale);
	    	} else if (params.get("locale") != null) {
	    		// Or loaded from the cookie 
	    		locale = (String) params.get("locale");
			    Tml.getLogger().debug("Cookie Locale: " + locale);
	    	} else {
	    		// Or taken from the Accepted Locale header 
	    		List<String> locales = new ArrayList<String>();
	    		@SuppressWarnings("unchecked")
				Enumeration<Locale> e = req.getLocales();
	    		while (e.hasMoreElements()) {
	    			locales.add(e.nextElement().getLanguage());
	    		}
	    		locale = Utils.join(locales, ",");
	    		Tml.getLogger().debug("Header Locale: " + locale);
	    	}
	    } else {
		    Tml.getLogger().debug("User Locale: " + locale);
	    }
	    params.put("locale", locale);
	    
    	String source = getCurrentSource(req);
	    if (source == null) {
	    	URL url = new URL(req.getRequestURL().toString());
	    	source = url.getPath();
		    Tml.getLogger().debug("Url Source: " + source);
	    } else {
		    Tml.getLogger().debug("User Source: " + source);
	    }
	    params.put("source", source);
	    	
	    return params;
    }
    
	/**
	 * Initializes the library
	 */
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    	Tml.getLogger().debug("Requesting " + request.getRequestURL().toString());
    	
    	Session tmlSession = null;
	    t0 = (new Date()).getTime();
	    
	    try {
		    tmlSession = new Session(prepareSessionParams(request, response));
		    request.setAttribute(TML_SESSION_KEY, tmlSession);
		} catch(Exception ex) {
			return false;
		}
	    
        return true;
    }

	/**
	 * Submits missing translations
	 */
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		
		Session tmlSession = (Session) request.getAttribute(TML_SESSION_KEY);
		
	    if (tmlSession != null) 
	    	tmlSession.getApplication().submitMissingTranslationKeys();
	}

	/**
	 * Calculates execution time
	 */
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)
			throws Exception {
		
		if (t0 != null) {
			Long t1 = (new Date()).getTime();
			Tml.getLogger().debug("Request took: " + (t1-t0) + " mls");
		}
	}
    
}