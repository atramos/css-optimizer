package com.philip.cssFilter;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import com.megaads.css.optimizer.CSSOptimizer;
import com.megaads.css.optimizer.CSSRule;

public class CssFilter extends HttpServlet implements Filter {

	private final Logger logger = java.util.logging.Logger.getLogger(getClass().getName());

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		logger.info("init ok");
	}

	@Override // Servlet API 3.1
	public void init(FilterConfig filterConfig) throws ServletException {
		logger.info("init ok");
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		String path = ((HttpServletRequest) request).getServletPath();
		String tPath = path.substring(path.length() - 4, path.length());

		if (tPath.matches(".*\\.html?$")) {

			// 1.Read the html and store in memory:
			PrintWriter out = response.getWriter();
			CustomResponseWrapper wrapper = new CustomResponseWrapper((HttpServletResponse) response);
			chain.doFilter(request, wrapper);

			CharArrayWriter writer = new CharArrayWriter();
			String originalContent = wrapper.getResponseContent();
			logger.info("Response Length = " + originalContent.length());

			// 2.optimize the css:
			Document doc = Jsoup.parse(originalContent);
			Elements links = doc.getElementsByTag("link");

			logger.info("Number of links = " + links.size());

			String changedContent = originalContent;
			for (Element link : links) {

				// whether is css:
				String type = link.attr("type");
				String rel = link.attr("rel");

				logger.finest("type=" + type + " rel=" + rel);

				if (!("text/css".equals(type) || "stylesheet".equals(rel)))
					continue;

				String linkHref = link.attr("href");
				if (linkHref == null || linkHref.length() == 0)
					continue;

				logger.info("begin to solve css: " + link.attr("href"));

				CSSOptimizer optimizer = new CSSOptimizer(originalContent);

				// 3.2 keep the class and tags:
				String keepClass = request.getServletContext().getInitParameter("keepClass");
				String keepTag = request.getServletContext().getInitParameter("keepTag");

				String[] classes;
				if (keepClass != null && keepClass.length() != 0) {
					classes = keepClass.split(",");
					for (int i = 0; i < classes.length; i++) {
						optimizer.keepClassName(classes[i]);
					}
				}
				String[] tags;
				if (keepTag != null && keepTag.length() != 0) {
					tags = keepTag.split(",");
					for (int i = 0; i < tags.length; i++) {
						optimizer.keepTagName(tags[i]);
					}
				}

				logger.info("keep the classes and tags.");

				// 2.5 used class and tag in html:
				optimizer.extractUsedClass4Content();
				logger.info("used classes and tags.");

				// 2.6 Get CSS url:
				String cssFile = retriveCSS(request, linkHref);
				if (cssFile == null || cssFile.length() == 0)
					continue;

				// 3.2 from used html to optimize css:
				List<CSSRule> cssRules = optimizer.extractCSSRules(cssFile);

				logger.info("Found " + optimizer.getCssStyleRulesCount() + " style rule(s); begin optimizing ...");
				String result = optimizer.buildResult(cssRules);
				logger.info("optimized the css file.");
				logger.info(result);

				// 3.3 replace html href with css:
				Element newNode = new Element(Tag.valueOf("style"), result);
				newNode.attr("type", "text/css");
				newNode.text(result);

				link.replaceWith(newNode);
			}
			
			// 4. write to response:
			Entities.EscapeMode.base.getMap().clear();
			writer.write(doc.toString());
			response.setContentLength(writer.toString().length());
			out.write(writer.toString());
			out.close();
		}
		else {
			chain.doFilter(request, response);
		}
		
	}

	public String retriveCSS(ServletRequest request, String linkHref) {
		String cssFile = "";
		String urlCss = "";
		String protocal = request.getProtocol();
		int index = protocal.indexOf("/");
		protocal = protocal.substring(0, index);
		String server = request.getServerName();
		int port = request.getLocalPort();
		String context = request.getServletContext().getContextPath();
		String rootURL = protocal + "://" + server + ":" + port + context;

		logger.info("The root path of files:" + rootURL);
		// Get CSS:
		if ('.' == linkHref.charAt(0)) {
			urlCss = rootURL + linkHref.substring(1, linkHref.length());
		} else if ("//".equals(linkHref.substring(0, 1))) {
			urlCss = rootURL + "/" + linkHref.substring(2, linkHref.length() - 1);
		} else if ('/' == linkHref.charAt(0)) {
			urlCss = rootURL + linkHref;
		} else if ("http".equals(linkHref.substring(0, 3))) {
			urlCss = linkHref;
		} else if (linkHref.length() != 0) {
			urlCss = rootURL + "/" + linkHref;
		} else {
			logger.info("----------------The CSS can't be retrived!-------------------");
			return cssFile;
		}

		logger.info("the css file's url is:" + urlCss);
		// Retrive CSS:
		HttpClient client = HttpClients.createDefault();
		HttpGet getMethod = new HttpGet(urlCss);

		StringBuffer sbCss = new StringBuffer();
		try {
			HttpResponse response = client.execute(getMethod);
			logger.info("The return code is : " + response.getStatusLine());
			if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
				return cssFile;

			BufferedReader inCss = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			String tempStr = "";
			while ((tempStr = inCss.readLine()) != null) {
				sbCss.append(tempStr);
				sbCss.append("\n");
			}
			inCss.close();
			cssFile = new String(sbCss.toString().getBytes(), "UTF-8");
			logger.info("the CSS file content: " + cssFile);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return cssFile;
	}

}
