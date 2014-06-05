package com.continuuity.security.guice;

import com.continuuity.common.conf.CConfiguration;
import com.continuuity.common.conf.Constants;
import com.continuuity.common.io.Codec;
import com.continuuity.security.auth.AccessToken;
import com.continuuity.security.auth.AccessTokenCodec;
import com.continuuity.security.auth.AccessTokenIdentifier;
import com.continuuity.security.auth.AccessTokenIdentifierCodec;
import com.continuuity.security.auth.AccessTokenTransformer;
import com.continuuity.security.auth.AccessTokenValidator;
import com.continuuity.security.auth.KeyIdentifier;
import com.continuuity.security.auth.KeyIdentifierCodec;
import com.continuuity.security.auth.TokenManager;
import com.continuuity.security.auth.TokenValidator;
import com.continuuity.security.server.AbstractAuthenticationHandler;
import com.continuuity.security.server.ExternalAuthenticationServer;
import com.continuuity.security.server.GrantAccessTokenHandler;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.PrivateModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.eclipse.jetty.server.Handler;

import java.util.HashMap;
import java.util.Map;

/**
 * Guice bindings for security related classes.  This extends {@code PrivateModule} in order to limit which classes
 * are exposed.
 */
public abstract class SecurityModule extends PrivateModule {

  @Override
  protected final void configure() {
    bind(new TypeLiteral<Codec<AccessToken>>() { }).to(AccessTokenCodec.class).in(Scopes.SINGLETON);
    bind(new TypeLiteral<Codec<AccessTokenIdentifier>>() { }).to(AccessTokenIdentifierCodec.class).in(Scopes.SINGLETON);
    bind(new TypeLiteral<Codec<KeyIdentifier>>() { }).to(KeyIdentifierCodec.class).in(Scopes.SINGLETON);

    bindKeyManager(binder());
    bind(TokenManager.class).in(Scopes.SINGLETON);

    bind(ExternalAuthenticationServer.class).in(Scopes.SINGLETON);

    MapBinder<String, Handler> handlerBinder = MapBinder.newMapBinder(binder(), String.class, Handler.class,
                                                                      Names.named("security.handlers.map"));

    handlerBinder.addBinding(ExternalAuthenticationServer.HandlerType.AUTHENTICATION_HANDLER)
                 .toProvider(AuthenticationHandlerProvider.class);
    handlerBinder.addBinding(ExternalAuthenticationServer.HandlerType.GRANT_TOKEN_HANDLER)
                 .to(GrantAccessTokenHandler.class);
    bind(HashMap.class).annotatedWith(Names.named("security.handlers"))
                       .toProvider(AuthenticationHandlerMapProvider.class)
                       .in(Scopes.SINGLETON);

    bind(TokenValidator.class).to(AccessTokenValidator.class);
    bind(AccessTokenTransformer.class).in(Scopes.SINGLETON);
    expose(AccessTokenTransformer.class);
    expose(TokenValidator.class);
    expose(TokenManager.class);
    expose(ExternalAuthenticationServer.class);
    expose(new TypeLiteral<Codec<KeyIdentifier>>() { });
    expose(new TypeLiteral<Codec<AccessTokenIdentifier>>() { });
  }

  @Provides
  private Class<Handler> provideHandlerClass(CConfiguration configuration) throws ClassNotFoundException {
    return (Class<Handler>) configuration.getClass(Constants.Security.AUTH_HANDLER_CLASS, null, Handler.class);
  }

  private static final class AuthenticationHandlerProvider implements  Provider<Handler> {

    private final Injector injector;
    private final Class<Handler> handlerClass;

    @Inject
    private AuthenticationHandlerProvider(Injector injector, Class<Handler> handlerClass) {
      this.injector = injector;
      this.handlerClass = handlerClass;
    }

    @Override
    public Handler get() {
      return injector.getInstance(handlerClass);
    }
  }

  private static final class AuthenticationHandlerMapProvider implements Provider<HashMap> {
    private final HashMap<String, Handler> handlerMap;

    @Inject
    public AuthenticationHandlerMapProvider(@Named("security.handlers.map") Map<String, Handler> handlers) {
      handlerMap = new HashMap<String, Handler>();
      Handler securityHandler = handlers.get(ExternalAuthenticationServer.HandlerType.AUTHENTICATION_HANDLER);
      Handler grantAccessTokenHandler = handlers.get(ExternalAuthenticationServer.HandlerType.GRANT_TOKEN_HANDLER);
      ((AbstractAuthenticationHandler) securityHandler).setHandler(grantAccessTokenHandler);

      handlerMap.put(ExternalAuthenticationServer.HandlerType.AUTHENTICATION_HANDLER, securityHandler);
      handlerMap.put(ExternalAuthenticationServer.HandlerType.GRANT_TOKEN_HANDLER, grantAccessTokenHandler);
    }

    @Override
    public HashMap get() {
      return handlerMap;
    }
  }

  protected abstract void bindKeyManager(Binder binder);
}
