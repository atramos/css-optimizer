# css-optimizer
[css-optimizer](https://github.com/thoqbk/css-optimizer) is a tool that helps you to remove `un-used css rules` (`extra css rules`) basing on sample html files of your website

## Usage as Servlet Filter

web.xml

```xml
<!DOCTYPE web-app PUBLIC
 "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN"
 "http://java.sun.com/dtd/web-app_2_3.dtd" >

<web-app>
	<display-name>Css Optimize Application</display-name>

	<context-param>
		<param-name>keepClass</param-name>
		<param-value>pagination,icon,alert,alert-danger,alert-success,active,voted,invalid,glyphicon,glyphicon-thumbs-down,glyphicon-thumbs-up,voteUp,messages,text-success,hover</param-value>
	</context-param>

	<context-param>
		<param-name>keepTag</param-name>
		<param-value>hr</param-value>
	</context-param>

	<filter>
		<filter-name>CssFilter</filter-name>
		<filter-class>com.philip.cssFilter.CssFilter</filter-class>
	</filter>

	<filter-mapping>
		<filter-name>CssFilter</filter-name>
		<url-pattern>/*</url-pattern>
	</filter-mapping>

</web-app>
```


## Usage as a Java Library

```java
//Step 01.
//Initialize CSSOptimizer with target css file (file will be optimized) 
//and output file (result file after optimizing target cssfile)
CSSOptimizer optimizer = new CSSOptimizer("bootstrap.css","bootstrap.min.css");

//Step 02. Sampling your website by downloading web pages or using http urls
optimizer.addHtmlFile("about-us.htm");
optimizer.addHtmlFile("home.htm");
optimizer.addHtmlFile("blog.htm");
optimizer.addHtmlFile("http://your-website.com/product/ipad");

//Step 03. Some classes or tags are added using Javascript code, declare them here:
optimizer.keepClassName("img-thumbnail");
optimizer.keepClassName("pagination");
optimizer.keepTagName("hr");
        
//Step 04. Execute optimizing, 
//result file will be stored in path that is declared in Step 01.
optimizer.optimize();
```

## Example

We used [css-optimizer](https://github.com/thoqbk/css-optimizer) to remove `un-used css rules` in library [bootstrap.css](http://getbootstrap.com/css/). 

As the result, `59%` rule has been removed, css file size reduced from `121KB` to `59KB` 
and increased about `5 score` when testing my site on [Google PageSpeed](https://developers.google.com/speed/pagespeed/insights/)

Please check out and run the test file [TestCSSOptimizer.java](https://github.com/thoqbk/css-optimizer/blob/master/src/test/java/com/megaads/css/optimizer/test/TestCSSOptimizer.java) to see example result

## Solution and design
The most important things that i use in this project are `Regular Expression` and [CSS Rule design](https://developer.mozilla.org/en-US/docs/Web/API/CSSRule). I use `Regular Expression` to split css rules and extract all information about rules and put them into object models (CSSStyleRule, NonCSSStyleRule and CSSMediaRule). Following is `class diagram` of [css-optimizer](https://github.com/thoqbk/css-optimizer):

![css-optimizer class diagram](https://github.com/thoqbk/css-optimizer/blob/master/src/main/resources/com/megaads/css/optimizer/resource/class-diagram.jpg)

Steps to optimize css file includes:
* Step 1. use [jsoup](http://jsoup.org/) to parse sampling html files and extract all used html tags and css classes
* Step 2. use `Regular Expression` to parse css file and put information into object models
* Step 3. compare the result from `Step 1.` and `Step 2.` to get only used css rules and write down them into the result file


## Author and contact
[ThoQ Luong](https://github.com/thoqbk/)

Email: thoqbk@gmail.com

Servlet Filter modifications: atramos (github)

## License

The MIT License (MIT)
