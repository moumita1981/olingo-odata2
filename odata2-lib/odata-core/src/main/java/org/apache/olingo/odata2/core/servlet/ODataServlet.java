package org.apache.olingo.odata2.core.servlet;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.olingo.odata2.api.ODataService;
import org.apache.olingo.odata2.api.ODataServiceFactory;
import org.apache.olingo.odata2.api.commons.HttpHeaders;
import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.commons.ODataHttpMethod;
import org.apache.olingo.odata2.api.exception.MessageReference;
import org.apache.olingo.odata2.api.exception.ODataBadRequestException;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.exception.ODataHttpException;
import org.apache.olingo.odata2.api.exception.ODataMethodNotAllowedException;
import org.apache.olingo.odata2.api.exception.ODataNotAcceptableException;
import org.apache.olingo.odata2.api.exception.ODataNotImplementedException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.processor.ODataRequest;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.core.ODataContextImpl;
import org.apache.olingo.odata2.core.ODataRequestHandler;
import org.apache.olingo.odata2.core.exception.ODataRuntimeException;

public class ODataServlet extends HttpServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private ODataServiceFactory serviceFactory;
  private int pathSplit = 0;

  @Override
  protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
    final String factoryClassName = getInitParameter(ODataServiceFactory.FACTORY_LABEL);
    if (factoryClassName == null) {
      throw new ODataRuntimeException("config missing: org.apache.olingo.odata2.processor.factory");
    }
    try {
      serviceFactory = (ODataServiceFactory) Class.forName(factoryClassName).newInstance();
    } catch (Exception e) {
      throw new ODataRuntimeException(e);
    }
    final String pathSplitAsString = getInitParameter(ODataServiceFactory.PATH_SPLIT_LABEL);
    if (pathSplitAsString != null) {
      pathSplit = Integer.parseInt(pathSplitAsString);
    }
    String xHttpMethod = req.getHeader("X-HTTP-Method");
    String xHttpMethodOverride = req.getHeader("X-HTTP-Method-Override");
    if (xHttpMethod != null && xHttpMethodOverride != null) {
      if (!xHttpMethod.equalsIgnoreCase(xHttpMethodOverride)) {
        ODataExceptionWrapper wrapper = new ODataExceptionWrapper(req);
        createResponse(resp, wrapper.wrapInExceptionResponse(
            new ODataBadRequestException(ODataBadRequestException.AMBIGUOUS_XMETHOD)));
      }
    }

    if (req.getPathInfo() == null) {
      handleRedirect(req, resp);
    } else {
      handle(req, resp, xHttpMethod, xHttpMethodOverride);
    }
  }

  private void handle(final HttpServletRequest req, final HttpServletResponse resp, final String xHttpMethod,
      final String xHttpMethodOverride) throws IOException {
    String method = req.getMethod();
    if (ODataHttpMethod.GET.name().equals(method)) {
      handleRequest(req, ODataHttpMethod.GET, resp);
    } else if (ODataHttpMethod.POST.name().equals(method)) {
      if (xHttpMethod == null && xHttpMethodOverride == null) {
        handleRequest(req, ODataHttpMethod.POST, resp);
      } else if (xHttpMethod == null && xHttpMethodOverride != null) {
        /* tunneling */
        boolean methodHandled = methodForTunneling(req, resp, xHttpMethodOverride);
        if (!methodHandled) {
          createMethodNotAllowedResponse(req, ODataHttpException.COMMON, resp);
        }
      } else {
        /* tunneling */
        boolean methodHandled = methodForTunneling(req, resp, xHttpMethod);
        if (!methodHandled) {
          createNotImplementedResponse(req, ODataNotImplementedException.TUNNELING, resp);
        }
      }

    } else if (ODataHttpMethod.PUT.name().equals(method)) {
      handleRequest(req, ODataHttpMethod.PUT, resp);
    } else if (ODataHttpMethod.DELETE.name().equals(method)) {
      handleRequest(req, ODataHttpMethod.DELETE, resp);
    } else if (ODataHttpMethod.PATCH.name().equals(method)) {
      handleRequest(req, ODataHttpMethod.PATCH, resp);
    } else if (ODataHttpMethod.MERGE.name().equals(method)) {
      handleRequest(req, ODataHttpMethod.MERGE, resp);
    } else if ("HEAD".equals(method) || "OPTIONS".equals(method)) {
      createNotImplementedResponse(req, ODataNotImplementedException.COMMON, resp);
    } else {
      createMethodNotAllowedResponse(req, ODataHttpException.COMMON, resp);
    }
  }

  private boolean methodForTunneling(final HttpServletRequest req, final HttpServletResponse resp,
      final String xHttpMethod) throws IOException {
    /* tunneling */
    if (ODataHttpMethod.MERGE.name().equals(xHttpMethod)) {
      handleRequest(req, ODataHttpMethod.MERGE, resp);

    } else if (ODataHttpMethod.PATCH.name().equals(xHttpMethod)) {
      handleRequest(req, ODataHttpMethod.PATCH, resp);

    } else if (ODataHttpMethod.DELETE.name().equals(xHttpMethod)) {
      handleRequest(req, ODataHttpMethod.DELETE, resp);

    } else if (ODataHttpMethod.PUT.name().equals(xHttpMethod)) {
      handleRequest(req, ODataHttpMethod.PUT, resp);

    } else if (ODataHttpMethod.GET.name().equals(xHttpMethod)) {
      handleRequest(req, ODataHttpMethod.GET, resp);

    } else if (ODataHttpMethod.POST.name().equals(xHttpMethod)) {
      handleRequest(req, ODataHttpMethod.POST, resp);

    } else if ("HEAD".equals(xHttpMethod) || "OPTIONS".equals(xHttpMethod)) {
      createNotImplementedResponse(req, ODataNotImplementedException.COMMON, resp);

    } else {
      return false;
    }
    return true;
  }

  private void handleRequest(final HttpServletRequest req, final ODataHttpMethod method, final HttpServletResponse resp)
      throws IOException {
    try {
      if (req.getHeader(HttpHeaders.ACCEPT) != null && req.getHeader(HttpHeaders.ACCEPT).isEmpty()) {
        createNotAcceptableResponse(req, ODataNotAcceptableException.COMMON, resp);
      }
      ODataRequest request = ODataRequest.method(method)
          .contentType(RestUtil.extractRequestContentType(req.getContentType()).toContentTypeString())
          .acceptHeaders(RestUtil.extractAcceptHeaders(req.getHeader(HttpHeaders.ACCEPT)))
          .acceptableLanguages(RestUtil.extractAcceptableLanguage(req.getHeader(HttpHeaders.ACCEPT_LANGUAGE)))
          .pathInfo(RestUtil.buildODataPathInfo(req, pathSplit))
          .queryParameters(RestUtil.extractQueryParameters(req.getQueryString()))
          .requestHeaders(RestUtil.extractHeaders(req))
          .body(req.getInputStream())
          .build();
      ODataContextImpl context = new ODataContextImpl(request, serviceFactory);
      ODataService service = serviceFactory.createService(context);
      context.setService(service);
      context.setParameter(ODataContext.HTTP_SERVLET_REQUEST_OBJECT, req);
      service.getProcessor().setContext(context);
      ODataRequestHandler requestHandler = new ODataRequestHandler(serviceFactory, service, context);
      final ODataResponse odataResponse = requestHandler.handle(request);
      createResponse(resp, odataResponse);
    } catch (ODataException e) {
      ODataExceptionWrapper wrapper = new ODataExceptionWrapper(req);
      createResponse(resp, wrapper.wrapInExceptionResponse(e));
    }
  }

  private void handleRedirect(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
    String method = req.getMethod();
    if (ODataHttpMethod.GET.name().equals(method) ||
        ODataHttpMethod.POST.name().equals(method) ||
        ODataHttpMethod.PUT.name().equals(method) ||
        ODataHttpMethod.DELETE.name().equals(method) ||
        ODataHttpMethod.PATCH.name().equals(method) ||
        ODataHttpMethod.MERGE.name().equals(method) ||
        "HEAD".equals(method) ||
        "OPTIONS".equals(method)) {
      ODataResponse odataResponse = ODataResponse.status(HttpStatusCodes.TEMPORARY_REDIRECT)
          .header(HttpHeaders.LOCATION, "/")
          .build();
      createResponse(resp, odataResponse);
    } else {
      createMethodNotAllowedResponse(req, ODataHttpException.COMMON, resp);
    }

  }

  private void createResponse(final HttpServletResponse resp, final ODataResponse response) throws IOException {

    resp.setStatus(response.getStatus().getStatusCode());
    resp.setContentType(response.getContentHeader());
    for (String headerName : response.getHeaderNames()) {
      resp.setHeader(headerName, response.getHeader(headerName));
    }

    Object entity = response.getEntity();
    if (entity != null) {
      ServletOutputStream out = resp.getOutputStream();
      int curByte = -1;
      if (entity instanceof InputStream) {
        while ((curByte = ((InputStream) entity).read()) != -1) {
          out.write((char) curByte);
        }
        ((InputStream) entity).close();
      } else if (entity instanceof String) {
        String body = (String) entity;
        byte[] byteArray = body.getBytes("utf-8");
        out.write(byteArray);
      }

      out.flush();
      out.close();
    }
  }

  private void createNotImplementedResponse(final HttpServletRequest req, final MessageReference messageReference,
      final HttpServletResponse resp) throws IOException {
    // RFC 2616, 5.1.1: "An origin server SHOULD return the status code [...]
    // 501 (Not Implemented) if the method is unrecognized [...] by the origin server."
    ODataExceptionWrapper exceptionWrapper = new ODataExceptionWrapper(req);
    ODataResponse response =
        exceptionWrapper.wrapInExceptionResponse(new ODataNotImplementedException(messageReference));
    createResponse(resp, response);
  }

  private void createMethodNotAllowedResponse(final HttpServletRequest req, final MessageReference messageReference,
      final HttpServletResponse resp) throws IOException {
    ODataExceptionWrapper exceptionWrapper = new ODataExceptionWrapper(req);
    ODataResponse response =
        exceptionWrapper.wrapInExceptionResponse(new ODataMethodNotAllowedException(messageReference));
    createResponse(resp, response);
  }

  private void createNotAcceptableResponse(final HttpServletRequest req, final MessageReference messageReference,
      final HttpServletResponse resp) throws IOException {
    ODataExceptionWrapper exceptionWrapper = new ODataExceptionWrapper(req);
    ODataResponse response =
        exceptionWrapper.wrapInExceptionResponse(new ODataNotAcceptableException(messageReference));
    createResponse(resp, response);

  }

}
