package com.miro.miroappoauth.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.miro.miroappoauth.client.MiroAuthClient
import com.miro.miroappoauth.client.MiroClient
import com.miro.miroappoauth.config.AppProperties
import com.miro.miroappoauth.dto.AccessTokenDto
import com.miro.miroappoauth.dto.UserDto
import com.miro.miroappoauth.model.SessionToken
import com.miro.miroappoauth.model.TokenState
import com.miro.miroappoauth.model.TokenState.INVALID
import com.miro.miroappoauth.model.TokenState.NEW
import com.miro.miroappoauth.model.TokenState.VALID
import com.miro.miroappoauth.utils.getCurrentRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.client.RestClientException
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.Instant
import java.util.Collections
import java.util.UUID
import javax.servlet.http.HttpSession

@Controller
class HomeController(
    private val appProperties: AppProperties,
    private val miroAuthClient: MiroAuthClient,
    private val miroClient: MiroClient,
    private val objectMapper: ObjectMapper
) {

    @GetMapping("/")
    fun indexPage(
        session: HttpSession,
        model: Model
    ): String? {
        initModelAttributes(session, model)
        val message = session.getAttribute(SESSION_ATTR_MESSAGE)
        if (message != null) {
            model.addAttribute("message", message)
            session.removeAttribute(SESSION_ATTR_MESSAGE)
        }
        return "index"
    }

    @GetMapping("/install")
    fun install(
        session: HttpSession,
        model: Model,
        @RequestParam("code") code: String,
        @RequestParam(name = "state", required = false) state: String?
    ): String {
        val userId = getUserId(session)
        // todo state signed by JWT
        if (state != userId) {
            throw IllegalArgumentException("Unexpected state $state, should be $userId")
        }

        // resolve value by current request URL, but omit query parameters
        val servletRequest = getCurrentRequest()
        val request = ServletServerHttpRequest(servletRequest)
        val redirectUri = UriComponentsBuilder.fromHttpRequest(request)
            .query(null)
            .toUriString()

        val accessToken = miroAuthClient.getAccessToken(code, redirectUri)
        storeToken(session, accessToken)

        val user = getSelfUser(session, accessToken.accessToken)

        if (accessToken.refreshToken != null) {
            // todo evict old token
            // todo additional UI action button
            refreshToken(session, accessToken)
        }

        session.setAttribute(SESSION_ATTR_MESSAGE, "Application successfully installed for ${user.name}")
        return "redirect:/"
    }

    private fun getSelfUser(session: HttpSession, accessToken: String): UserDto {
        try {
            val self = miroClient.getSelfUser(accessToken)
            updateToken(session, accessToken, VALID)
            return self
        } catch (e: RestClientException) {
            updateToken(session, accessToken, INVALID)
            throw e
        }
    }

    private fun refreshToken(session: HttpSession, accessToken: AccessTokenDto): AccessTokenDto {
        try {
            if (accessToken.refreshToken == null) {
                throw IllegalStateException("refresh_token is null for $accessToken")
            }
            val refreshedToken = miroAuthClient.refreshToken(accessToken.refreshToken)
            storeToken(session, refreshedToken)
            return refreshedToken
        } catch (e: RestClientException) {
            updateToken(session, accessToken.accessToken, INVALID)
            throw e
        }
    }

    private fun storeToken(
        session: HttpSession,
        accessToken: AccessTokenDto
    ) {
        // usually we should not store DTO objects, just to simplify for now
        val attrName = "$ATTR_ACCESS_TOKEN_PREFIX${accessToken.accessToken}"
        session.setAttribute(attrName, SessionToken(accessToken, NEW, null))
    }

    private fun updateToken(session: HttpSession, accessToken: String, state: TokenState) {
        val attrName = "$ATTR_ACCESS_TOKEN_PREFIX$accessToken"
        val sessionToken = session.getAttribute(attrName) as SessionToken
        sessionToken.state = state
        sessionToken.lastAccessedTime = Instant.now()
        session.setAttribute(attrName, sessionToken)
    }

    private fun initModelAttributes(session: HttpSession, model: Model) {
        val servletRequest = getCurrentRequest()
        val request = ServletServerHttpRequest(servletRequest)
        // note: we call UriComponentsBuilder.fromHttpRequest here
        // to resolve ngrok-proxied Host header
        // Alternative solution: "server.forward-headers-strategy: framework" in yaml config
        val redirectUri = UriComponentsBuilder.fromHttpRequest(request)
            .replacePath("/install")
            .query(null)
            .build().toUri()
        val webPlugin = UriComponentsBuilder.fromHttpRequest(request)
            .replacePath("/webapp-sdk1/index.html")
            .query(null)
            .build().toUri()

        val referer = servletRequest.getHeader(HttpHeaders.REFERER)
        val userId = getUserId(session)

        model.addAttribute("sessionId", session.id)
        model.addAttribute("userId", userId)
        model.addAttribute("miroBaseUrl", appProperties.miroBaseUrl)
        model.addAttribute("clientId", appProperties.clientId)
        model.addAttribute("redirectUri", redirectUri)
        model.addAttribute("webPlugin", webPlugin)
        model.addAttribute("authorizeUrl", getAuthorizeUrl(redirectUri, state = userId))
        model.addAttribute("installationManagementUrl", getInstallationManagementUrl(appProperties.teamId))
        model.addAttribute("referer", referer)

        val accessTokens = Collections.list(session.attributeNames)
            .filter { it.startsWith(ATTR_ACCESS_TOKEN_PREFIX) }
            .map { session.getAttribute(it) }
            .map { objectMapper.writeValueAsString(it) }
        model.addAttribute("accessTokens", accessTokens)
    }

    private fun getInstallationManagementUrl(teamId: Long?): String? {
        if (teamId == null) {
            return null
        }
        return UriComponentsBuilder.fromHttpUrl(appProperties.miroBaseUrl)
            .path("/app/settings/team/{teamId}/app-settings/{clientId}")
            .buildAndExpand(teamId, appProperties.clientId)
            .toUriString()
    }

    private fun getAuthorizeUrl(redirectUri: URI, state: String): String {
        return UriComponentsBuilder.fromHttpUrl(appProperties.miroBaseUrl)
            .path("/oauth/authorize")
            .queryParam("response_type", "code")
            .queryParam("client_id", appProperties.clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("state", state)
            .apply {
                if (appProperties.teamId != null) {
                    queryParam("team_id", appProperties.teamId)
                }
            }
            .build(false)
            .encode()
            .toUriString()
    }
}

fun getUserId(session: HttpSession): String {
    var userId = session.getAttribute(SESSION_ATTR_USER_ID) as String?
    if (userId == null) {
        userId = UUID.randomUUID().toString()
        session.setAttribute(SESSION_ATTR_USER_ID, userId)
    }
    return userId
}

const val SESSION_ATTR_USER_ID = "user_id"

const val SESSION_ATTR_MESSAGE = "message"

const val ATTR_ACCESS_TOKEN_PREFIX = "accessToken-"
