package com.web.config;

import static com.web.domain.enums.SocialType.FACEBOOK;
import static com.web.domain.enums.SocialType.GOOGLE;
import static com.web.domain.enums.SocialType.KAKAO;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.filter.CharacterEncodingFilter;

import com.web.oauth2.CustomOAuth2Provider;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	

    @Override
	protected void configure(HttpSecurity http) throws Exception {
    	CharacterEncodingFilter filter = new CharacterEncodingFilter();
    	
    	http
    		.authorizeRequests()
    			.antMatchers("/","/oauth2/**","/login/**","/css/**","/images/**","/js/**","/console/**").permitAll()
    			.antMatchers("/facebook").hasAuthority(FACEBOOK.getRoleType())
                .antMatchers("/google").hasAuthority(GOOGLE.getRoleType())
                .antMatchers("/kakao").hasAuthority(KAKAO.getRoleType())
    			.anyRequest().authenticated()
    		.and()
    			.oauth2Login()
    			.defaultSuccessUrl("/loginSuccess")
    			.failureUrl("/loginFailure")
    		.and()
    			.headers().frameOptions().disable()
    		.and()
    			.exceptionHandling()
    			.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
    		.and()
    			.formLogin()
    			.successForwardUrl("/board/list")
    		.and()
    			.logout()
    			.logoutUrl("/logout")
    			.logoutSuccessUrl("/login")
    			.deleteCookies("JSESSIONID")
    			.invalidateHttpSession(true)
    		.and()
    			.addFilterBefore(filter, CsrfFilter.class)
    			.csrf().disable();
	}
    
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
    		OAuth2ClientProperties oAuth2ClientProperties,
    		@Value("${custom.oauth2.kakao.client-id}") String kakaoClientId) {
    	List<ClientRegistration> registrations = oAuth2ClientProperties.getRegistration().keySet().stream()
    			.map(client -> getRegistration(oAuth2ClientProperties, client))
    			.filter(Objects::nonNull)
    			.collect(Collectors.toList());
    	
    	registrations.add(CustomOAuth2Provider.KAKAO.getBuilder("kakao")
	    	.clientId(kakaoClientId)
	    	.clientSecret("test")
	    	.jwkSetUri("test")
	    	.build()
	    );
    	
    	return new InMemoryClientRegistrationRepository(registrations);
    }

	private ClientRegistration getRegistration(OAuth2ClientProperties clientProperties, String client) {
		
		if ("google".equals(client)) {
			OAuth2ClientProperties.Registration registration = clientProperties.getRegistration().get(client);
			return CommonOAuth2Provider.GOOGLE.getBuilder(client)
					.clientId(registration.getClientId())
					.clientSecret(registration.getClientSecret())
					.scope("email","profile")
					.build();
		}else if ("facebook".equals(client)) {
			OAuth2ClientProperties.Registration registration = clientProperties.getRegistration().get(client);
			return CommonOAuth2Provider.FACEBOOK.getBuilder(client)
					.clientId(registration.getClientId())
					.clientSecret(registration.getClientSecret())
					.userInfoUri("https://graph.facebook.com/me?fields=id,email,link")
					.scope("email")
					.build();
		}
		
		return null;
	}
}