package org.example.expert.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class AdminAccessAOP {
    /**
     * 레벨 2-9 AOP
     * 조건 : 특정 API에 접근할 때 접근 로그를 기록한다.
     * 로그 내용 : 사용자 ID, 요청 시각, 요청 URL
     */
    @Pointcut("execution(* org.example.expert.domain.comment.controller.CommentAdminController.deleteComment(..))")
    private void commentAdminControllerPointcut() {}

    @Pointcut("execution(* org.example.expert.domain.user.controller.UserAdminController.changeUserRole(..))")
    private void userAdminController() {}

    @Around("commentAdminControllerPointcut() || userAdminController()")
    public Object adminLog(ProceedingJoinPoint joinPoint) throws Throwable {
        // 참고 : https://whitelife.tistory.com/214
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        Long userId = (Long) request.getAttribute("userId");

        log.info("사용자 ID : {}, 요청 시각 : {}, 요청 URL : {}",
                userId, LocalDateTime.now(), request.getRequestURI());

        return  joinPoint.proceed();
    }
}
