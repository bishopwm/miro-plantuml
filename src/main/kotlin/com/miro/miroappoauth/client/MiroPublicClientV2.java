package com.miro.miroappoauth.client;

import com.miro.miroappoauth.client.dtov2.CreateShapeReq;
import com.miro.miroappoauth.client.dtov2.IdResp;
import com.miro.miroappoauth.dto.CreateImageReq;
import com.miro.miroappoauth.dto.CreateTextReq;
import com.miro.miroappoauth.dto.CreateTextResp;
import com.miro.miroappoauth.dto.UserDto;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

/**
 * See <a href="https://beta.developers.miro.com/reference/api-reference">miro api reference</a>.
 * Note: we use camelCase for json here.
 */
public class MiroPublicClientV2 {

    private final RestTemplate rest;

    public MiroPublicClientV2(RestTemplate rest) {
        this.rest = rest;
    }

    public UserDto getSelfUser(String accessToken, long userId)  {
        var headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        var request = new HttpEntity<>(null, headers);

        return rest.exchange("/v2/users/{userId}", GET, request, UserDto.class, userId).getBody();
    }

    public IdResp createShape(
            String accessToken,
            String boardId,
            CreateShapeReq createShapeReq
    ) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        var request = new HttpEntity<>(createShapeReq, headers);
        return rest.exchange("/v2/boards/{board_id}/shapes", POST, request, IdResp.class, boardId).getBody();
    }

    public IdResp createImage(
            String accessToken,
            String boardId,
            CreateImageReq createImageReq
    ) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        var request = new HttpEntity<>(createImageReq, headers);
        return rest.exchange("/v2/boards/{board_id}/images", POST, request, IdResp.class, boardId).getBody();
    }

    public CreateTextResp createText(
            String accessToken,
            String boardId,
            CreateTextReq createTextReq
    ) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        var request = new HttpEntity<>(createTextReq, headers);
        return rest.exchange("/v2/boards/{board_id}/texts", POST, request, CreateTextResp.class, boardId).getBody();
    }
}
