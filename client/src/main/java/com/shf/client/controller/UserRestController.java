package com.shf.client.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shf.entity.Foo;
import com.shf.entity.User;
import com.shf.entity.UserRequest;

import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.shf.mimetype.MimeTypes.FOO_MIME_TYPE;
import static com.shf.mimetype.MimeTypes.MAP_MIME_TYPE;
import static com.shf.mimetype.MimeTypes.PARAMETERIZED_TYPE_MIME_TYPE;
import static com.shf.mimetype.MimeTypes.REFRESH_TOKEN_MIME_TYPE;
import static com.shf.mimetype.MimeTypes.SECURITY_TOKEN_MIME_TYPE;

/**
 * Description:
 * WebFlux server.
 *
 * @author: songhaifeng
 * @date: 2019/11/18 15:13
 */
@RestController
@RequestMapping("/user")
public class UserRestController {

    private final RSocketRequester rSocketRequester1;

    private final RSocketRequester rSocketRequester2;

    private final ObjectMapper objectMapper;

    @Autowired
    public UserRestController(@Qualifier("rSocketRequester1") RSocketRequester rSocketRequester1,
                              @Qualifier("rSocketRequester2") RSocketRequester rSocketRequester2,
                              ObjectMapper objectMapper) {
        this.rSocketRequester1 = rSocketRequester1;
        this.rSocketRequester2 = rSocketRequester2;
        this.objectMapper = objectMapper;
    }

    /***********************************request/response ******************************/
    /**
     * call the retrieveMono() method, Spring Boot initiates a request/response interaction.
     *
     * @param id id
     * @return user
     */
    @GetMapping(value = "{id}")
    public Publisher<User> user(@PathVariable("id") int id) {
        return rSocketRequester1
                .route("user")
                .data(new UserRequest(id))
                .retrieveMono(User.class);
    }

    /***********************************Fire And Forget******************************/

    /**
     * using the send() method to initiate the request instead of retrieveMono(),
     * the interaction model becomes fire-and-forget.
     *
     * @return void
     */
    @GetMapping(value = "add")
    public Publisher<Void> add() {
        return rSocketRequester1
                .route("add.user")
                .data(User.builder().id(4).age(12).name("ball").build())
                .send();
    }

    /***********************************Request Stream******************************/
    /**
     * Defining our response expectation with the retrieveFlux() method call.
     * This is the part which determines the interaction model.
     * <p>
     * Also note that, since our client is also a REST server,
     * it defines response media type as MediaType.TEXT_EVENT_STREAM_VALUE.
     *
     * @return
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Publisher<User> list() {
        return rSocketRequester1
                .route("list")
                .retrieveFlux(User.class);
    }

    /***********************************Request Channel******************************/

    /**
     * If the data(payload) is a stream, then retrieveFlux() method call will be Request-Channel interaction model.
     *
     * @return User
     */
    @GetMapping(value = "request/channel", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Publisher<User> requestChannel() {
        return rSocketRequester1
                .route("request.channel")
                .data(Flux.interval(Duration.ofSeconds(1))
                        .map(i -> User.builder().id(i.intValue() + 5).name("bob").age(11).build())
                        .take(10))
                .retrieveFlux(User.class);
    }

    /***********************************Invoke Error******************************/
    @GetMapping("error")
    public Publisher<User> error() {
        return rSocketRequester1
                .route("user.error")
                .retrieveMono(User.class);
    }


    /***********************************Send metadata(header)******************************/
    /**
     * Send a String type of request header(metadata).
     *
     * @return String
     */
    @GetMapping(value = "send/string/header")
    public Publisher<String> sendStringHeader() {
        final String securityToken = "bearer token_001";
        final String refreshToken = "refresh_token_001";
        return rSocketRequester1
                .route("send.string.header")
                .data(new UserRequest(1))
                .metadata(securityToken, SECURITY_TOKEN_MIME_TYPE)
                .metadata(refreshToken, REFRESH_TOKEN_MIME_TYPE)
                .retrieveMono(String.class);
    }

    /**
     * Send three types of request headers(metadata), as String ,custom entity, Map
     *
     * @return void
     */
    @GetMapping(value = "send/headers")
    public Mono<Void> sendHeaders() {
        final String securityToken = "bearer token_001";
        return rSocketRequester1
                .route("send.headers")
                .metadata(securityToken, SECURITY_TOKEN_MIME_TYPE)
                .metadata(Foo.builder().name("foo001").build(), PARAMETERIZED_TYPE_MIME_TYPE)
                .metadata(buildMapHeader(), PARAMETERIZED_TYPE_MIME_TYPE)
                .send();
    }

    /**
     * Send custom entity for request header(metadata)
     *
     * @return String
     */
    @GetMapping(value = "send/entity/header")
    public Publisher<String> sendEntityHeader() {
        return rSocketRequester1
                .route("send.entity.header")
                .metadata(Foo.builder().name("foo001").build(), FOO_MIME_TYPE)
                .retrieveMono(String.class);
    }

    /**
     * Send a Map type of request header(metadata).
     *
     * @return String
     */
    @GetMapping(value = "send/map/header")
    public Publisher<String> sendMapHeader() {
        return rSocketRequester1
                .route("send.map.header")
                .metadata(buildMapHeader(), MAP_MIME_TYPE)
                .retrieveMono(String.class);
    }

    /**
     * Send a json-string of request header(metadata).
     *
     * @return String
     */
    @GetMapping(value = "send/json/header")
    public Publisher<String> sendJsonHeader() throws JsonProcessingException {
        return rSocketRequester1
                .route("send.entity.header")
                .metadata(objectMapper.writeValueAsString(Foo.builder().name("foo001").build()), FOO_MIME_TYPE)
                .retrieveMono(String.class);
    }

    /***********************************DestinationVariable******************************/
    /**
     * Test destination variable in route.
     *
     * @return User
     */
    @GetMapping(value = "another/{id}")
    public Publisher<User> destinationVariable(@PathVariable int id) {
        return rSocketRequester1
                .route("user." + id)
                .retrieveMono(User.class);
    }

    /***********************************Test Responder******************************/
    @GetMapping(value = "requester1/responder")
    public Publisher<String> replayRequester1() {
        final String securityToken = "bearer token_001";
        final String refreshToken = "refresh_token_001";
        return rSocketRequester1
                .route("requester.responder")
                .data(new UserRequest(1))
                .metadata(securityToken, SECURITY_TOKEN_MIME_TYPE)
                .metadata(refreshToken, REFRESH_TOKEN_MIME_TYPE)
                .retrieveMono(String.class);
    }

    @GetMapping(value = "requester2/responder")
    public Publisher<String> replayRequester2() {
        final String securityToken = "bearer token_002";
        final String refreshToken = "refresh_token_002";
        return rSocketRequester2
                .route("requester.responder")
                .data(new UserRequest(2))
                .metadata(securityToken, SECURITY_TOKEN_MIME_TYPE)
                .metadata(refreshToken, REFRESH_TOKEN_MIME_TYPE)
                .retrieveMono(String.class);
    }


    private Map<String, Object> buildMapHeader() {
        Map<String, Object> map = new HashMap<>(2);
        map.put("foo", "bar");
        map.put("fto", Foo.builder().name("car").build());
        return map;
    }
}
