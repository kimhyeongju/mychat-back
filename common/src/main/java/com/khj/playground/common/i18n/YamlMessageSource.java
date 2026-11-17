package com.khj.playground.common.i18n;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * yml 로 쓴 메시지를 읽는 MessageSource.
 *
 * 각 모듈이 같은 경로에 파일을 둔다. classpath*: 로 모두 모아 합친다.
 *   i18n/exception_ko.yml, i18n/exception_en.yml   오류 코드와 문구
 *   i18n/valid-message_ko.yml, valid-message_en.yml  입력 검증 문구
 * 중첩 키는 점으로 이어 붙인 한 줄 키가 된다. (user-dto.email.email, login-failed.message)
 *
 * 선택한 언어에 키가 없으면 한국어로, 그것도 없으면 null 을 돌려준다.
 * 인자가 없는 조회는 MessageFormat 을 거치지 않으므로 검증 문구의 {min}, {max} 는 Hibernate Validator 가 채운다.
 */
public class YamlMessageSource extends AbstractMessageSource {

  private static final List<String> LANGUAGES = List.of("ko", "en");
  private static final List<String> BASENAMES = List.of("exception", "valid-message");

  private final Map<String, Map<String, String>> messages = new HashMap<>();

  public YamlMessageSource() {
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    for (String language : LANGUAGES) {
      List<Resource> resources = new ArrayList<>();
      for (String base : BASENAMES) {
        try {
          resources.addAll(
            Arrays.asList(resolver.getResources("classpath*:i18n/" + base + "_" + language + ".yml"))
          );
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
      Map<String, String> flat = new HashMap<>();
      if (!resources.isEmpty()) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(resources.toArray(new Resource[0]));
        Properties properties = yaml.getObject();
        if (properties != null) {
          properties.forEach((k, v) -> flat.put(String.valueOf(k), String.valueOf(v)));
        }
      }
      messages.put(language, flat);
    }
    setUseCodeAsDefaultMessage(false);
  }

  @Override
  protected String resolveCodeWithoutArguments(String code, Locale locale) {
    return lookup(code, locale);
  }

  @Override
  protected MessageFormat resolveCode(String code, Locale locale) {
    String pattern = lookup(code, locale);
    if (pattern == null) {
      return null;
    }
    // MessageFormat 에서 작은따옴표는 특수 문자다. 문구에 쓰는 따옴표(can't 등)가 사라지지 않게 이스케이프한다.
    return new MessageFormat(pattern.replace("'", "''"), locale);
  }

  private String lookup(String code, Locale locale) {
    String language = "en".equals(locale.getLanguage()) ? "en" : "ko";
    String value = messages.get(language).get(code);
    if (value == null && !"ko".equals(language)) {
      value = messages.get("ko").get(code);
    }
    return value;
  }
}
