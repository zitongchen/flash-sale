package com.actionworks.flashsale.controller.config;

import com.actionworks.flashsale.app.auth.AuthorizationService;
import com.actionworks.flashsale.app.auth.model.AuthResult;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

/**
 * todo: HandlerInterceptor 和 HandlerInterceptorAdapter 的使用效果是一致的，HandlerInterceptor 是接口，之前是需要进行实现的，
 * 不过现在接口可以有默认的方法，所以也可以跟 HandlerInterceptorAdapter 一样，选择需要的接口进行实现。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    private static final String USER_ID = "userId";

    @Resource
    private AuthorizationService authorizationService;

    /**
     * 方法返回 true 表示继续执行下去
     *
     * @param request
     * @param response
     * @param handler
     * @return
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object userId = request.getAttribute(USER_ID);
        if (userId != null) {
            return true;
        }
        String token = request.getParameter("token");
        AuthResult authResult = authorizationService.auth(token);
        if (authResult.isSuccess()) {
            // todo: 这里为什么需要使用 HttpServletRequest 的包装类呢？
            HttpServletRequestWrapper authRequestWrapper = new HttpServletRequestWrapper(request);
            authRequestWrapper.setAttribute(USER_ID, authResult.getUserId());
        }
        return true;
    }
}
