/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.oauth2.restlet;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.forgerock.oauth2.core.*;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.openam.oauth2.OAuth2Utils;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.utils.StringUtils;
import org.json.JSONException;
import org.restlet.Request;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.forgerock.oauth2.core.OAuth2Constants.Custom.*;
import static org.forgerock.oauth2.core.OAuth2Constants.DeviceCode.*;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.*;
import static org.forgerock.openam.utils.StringUtils.isEmpty;

/**
 * A Restlet resource for issuing new device codes.
 *
 * @since 13.0.0
 */
public class CodeResource extends ServerResource {

    public final static String WEI_BO = "WEI_BO";
    public final static String WECHAT = "WECHAT";

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final OAuth2RequestFactory<?, Request> requestFactory;
    private final ExceptionHandler exceptionHandler;
    private final JacksonRepresentationFactory jacksonRepresentationFactory;

    @Inject
    public CodeResource(ExceptionHandler exceptionHandler, OAuth2RequestFactory<?, Request> requestFactory,
                        JacksonRepresentationFactory jacksonRepresentationFactory) {
        this.exceptionHandler = exceptionHandler;
        this.requestFactory = requestFactory;
        this.jacksonRepresentationFactory = jacksonRepresentationFactory;
    }

    @Post
    public Representation issueCode(Representation body)
            throws OAuth2RestletException, UnirestException, JSONException {
        final Request restletRequest = getRequest();
        OAuth2Request request = requestFactory.create(restletRequest);

        String socialCode = request.getParameter(SOCIAL_CODE);
        String socialType = request.getParameter(SOCIAL_TYPE);

        if (!socialType.equals(WEI_BO)) {
            return jacksonRepresentationFactory.create("Only support login by Wei Bo for now");
        }

        JsonNode uidNode = Unirest.get("https://api.weibo.com/2/account/get_uid.json")
                .queryString("access_token", socialCode)
                .asJson().getBody();

        String uid = uidNode.getObject().getString("uid");

        HttpResponse<String> userInfo = Unirest.get("https://api.weibo.com/2/users/show.json")
                .queryString("access_token", socialCode)
                .queryString("uid", uid)
                .asString();

        Map<String, Object> result = new HashMap<>();
        result.put("code", UUID.randomUUID());
        result.put("userInfo", userInfo.getBody());

        return jacksonRepresentationFactory.create(result);
    }

    @Override
    protected void doCatch(Throwable throwable) {
        if (!(throwable.getCause() instanceof OAuth2RestletException)) {
            logger.error("Exception when issuing device tokens", throwable.getCause());
        }
        exceptionHandler.handle(throwable, getResponse());
    }
}
