package com.chit.app.domain.auth.presentation.resolver

import com.chit.app.domain.auth.domain.exception.AuthenticatedUserNotFoundException
import com.chit.app.global.common.annotation.CurrentMemberId
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Component
class CurrentUserArgumentResolver : HandlerMethodArgumentResolver, WebMvcConfigurer {
    
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(CurrentMemberId::class.java)
    }
    
    override fun resolveArgument(
            parameter: MethodParameter,
            mavContainer: ModelAndViewContainer?,
            webRequest: NativeWebRequest,
            binderFactory: WebDataBinderFactory?
    ): Any {
        return SecurityContextHolder.getContext().authentication
                ?.takeIf { it.isAuthenticated }
                ?.principal
                .let { principal -> (principal as? Long) ?: throw AuthenticatedUserNotFoundException() }
    }
    
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(this)
    }
    
}