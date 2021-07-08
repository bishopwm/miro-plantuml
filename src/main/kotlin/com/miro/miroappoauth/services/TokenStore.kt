package com.miro.miroappoauth.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.miro.miroappoauth.dto.AccessTokenDto
import com.miro.miroappoauth.model.Token
import com.miro.miroappoauth.model.TokenState
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.ResultSet

@Service
class TokenStore(
    private val objectMapper: ObjectMapper,
    private val jdbc: JdbcTemplate
) {

    fun insert(token: Token) {
        jdbc.update(
            "INSERT INTO token(" +
                "access_token, access_token_payload, client_id, user_id, team_id, " +
                "state, created_time, last_accessed_time) VALUES (" +
                "?, ?, ?, ?, ?, ?, ?, ?)",
            token.accessTokenValue(),
            objectMapper.writeValueAsString(token.accessToken),
            token.clientId,
            token.accessToken.userId,
            token.accessToken.teamId,
            token.state.name,
            token.createdTime,
            token.lastAccessedTime
        )
    }

    fun update(token: Token) {
        jdbc.update(
            "UPDATE token SET " +
                "access_token_payload = ?, " +
                "client_id = ?, " +
                "user_id = ?, " +
                "team_id = ?, " +
                "state = ?, " +
                "created_time = ?, " +
                "last_accessed_time = ? " +
                "WHERE access_token = ?",
            objectMapper.writeValueAsString(token.accessToken),
            token.clientId,
            token.accessToken.userId,
            token.accessToken.teamId,
            token.state.name,
            token.createdTime,
            token.lastAccessedTime,
            token.accessTokenValue()
        )
    }

    fun get(accessToken: String): Token? {
        return jdbc.query(
            "SELECT access_token, access_token_payload, client_id, state, created_time, last_accessed_time " +
                "FROM token WHERE access_token = ?",
            mapToken(), accessToken
        ).firstOrNull()
    }

    fun get(clientId: Long, userId: Long, teamId: Long): Token? {
        return jdbc.query(
            "SELECT access_token, access_token_payload, client_id, state, created_time, last_accessed_time " +
                "FROM token WHERE " +
                "client_id = ? AND user_id = ? AND team_id = ? " +
                "ORDER BY created_time DESC",
            mapToken(), clientId, userId, teamId
        ).firstOrNull()
    }

    private fun mapToken() = { rs: ResultSet, _: Int ->
        val accessTokenPayload = objectMapper.readValue(
            rs.getString("access_token_payload"),
            AccessTokenDto::class.java
        )
        val state = TokenState.valueOf(rs.getString("state"))
        Token(
            accessToken = accessTokenPayload,
            clientId = rs.getLong("client_id"),
            state = state,
            createdTime = rs.getTimestamp("created_time").toInstant(),
            lastAccessedTime = rs.getTimestamp("last_accessed_time")?.toInstant()
        )
    }
}
