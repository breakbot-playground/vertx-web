package io.vertx.ext.web.validation.impl;

import io.vertx.ext.web.validation.MalformedValueException;
import io.vertx.ext.web.validation.ParameterProcessorException;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.impl.parameter.ParameterParser;
import io.vertx.ext.web.validation.impl.parameter.ParameterProcessor;
import io.vertx.ext.web.validation.impl.parameter.ParameterProcessorImpl;
import io.vertx.ext.web.validation.impl.validator.ValueValidator;
import io.vertx.json.schema.ValidationException;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class ParameterProcessorUnitTest {

  @Mock
  ParameterParser mockedParser;
  @Mock
  ValueValidator mockedValidator;

  @Test
  public void testRequiredParam() {
    ParameterProcessor processor = new ParameterProcessorImpl(
      "myParam",
      ParameterLocation.QUERY,
      false,
      mockedParser,
      mockedValidator
    );

    when(mockedParser.parseParameter(any())).thenReturn(null);
    assertThatCode(() -> processor.process(new HashMap<>()))
      .isInstanceOf(ParameterProcessorException.class)
      .hasFieldOrPropertyWithValue("errorType", ParameterProcessorException.ParameterProcessorErrorType.MISSING_PARAMETER_WHEN_REQUIRED_ERROR)
      .hasFieldOrPropertyWithValue("location", ParameterLocation.QUERY)
      .hasFieldOrPropertyWithValue("parameterName", "myParam")
      .hasNoCause();
  }

  @Test
  public void testOptionalParam(VertxTestContext testContext) {
    ParameterProcessor processor = new ParameterProcessorImpl(
      "myParam",
      ParameterLocation.QUERY,
      true,
      mockedParser,
      mockedValidator
    );

    when(mockedParser.parseParameter(any())).thenReturn(null);
    when(mockedValidator.getDefault()).thenReturn(null);

    RequestParameter rp = processor.process(new HashMap<>());
    testContext.verify(() ->
      assertThat(rp).isNull()
    );
    testContext.completeNow();
  }


  @Test
  public void testOptionalParamWithDefault(VertxTestContext testContext) {
    ParameterProcessor processor = new ParameterProcessorImpl(
      "myParam",
      ParameterLocation.QUERY,
      true,
      mockedParser,
      mockedValidator
    );

    when(mockedParser.parseParameter(any())).thenReturn(null);
    when(mockedValidator.getDefault()).thenReturn("bla");

    RequestParameter rp = processor.process(new HashMap<>());
    testContext.verify(() ->
      assertThat(rp.getString()).isEqualTo("bla")
    );
    testContext.completeNow();
  }

  @Test
  public void testParsingFailure() {
    ParameterProcessor processor = new ParameterProcessorImpl(
      "myParam",
      ParameterLocation.QUERY,
      false,
      mockedParser,
      mockedValidator
    );

    when(mockedParser.parseParameter(any())).thenThrow(new MalformedValueException("bla"));

    assertThatCode(() -> processor.process(new HashMap<>()))
      .isInstanceOf(ParameterProcessorException.class)
      .hasFieldOrPropertyWithValue("errorType", ParameterProcessorException.ParameterProcessorErrorType.PARSING_ERROR)
      .hasFieldOrPropertyWithValue("location", ParameterLocation.QUERY)
      .hasFieldOrPropertyWithValue("parameterName", "myParam")
      .hasCauseInstanceOf(MalformedValueException.class);
  }

  @Test
  public void testValidation(VertxTestContext testContext) {
    ParameterProcessor processor = new ParameterProcessorImpl(
      "myParam",
      ParameterLocation.QUERY,
      true,
      mockedParser,
      mockedValidator
    );

    when(mockedParser.parseParameter(any())).thenReturn("aaa");
    when(mockedValidator.validate(any())).thenReturn(RequestParameter.create("aaa"));

    RequestParameter rp = processor.process(new HashMap<>());
    testContext.verify(() -> {
      assertThat(rp.isString()).isTrue();
      assertThat(rp.getString()).isEqualTo("aaa");
    });
    testContext.completeNow();
  }

  @Test
  public void testValidationFailure(VertxTestContext testContext) {
    ParameterProcessor processor = new ParameterProcessorImpl(
      "myParam",
      ParameterLocation.QUERY,
      true,
      mockedParser,
      mockedValidator
    );

    when(mockedParser.parseParameter(any())).thenReturn("aaa");
    when(mockedValidator.validate(any())).thenThrow(ValidationException.create("aaa", "aaa", "aaa"));

    try {
      processor.process(new HashMap<>());
      testContext.failNow("should not reach this");
    } catch (ParameterProcessorException err) {
      testContext.verify(() -> assertThat(err)
        .isInstanceOf(ParameterProcessorException.class)
        .hasFieldOrPropertyWithValue("errorType", ParameterProcessorException.ParameterProcessorErrorType.VALIDATION_ERROR)
        .hasFieldOrPropertyWithValue("location", ParameterLocation.QUERY)
        .hasFieldOrPropertyWithValue("parameterName", "myParam")
        .hasCauseInstanceOf(ValidationException.class));
      testContext.completeNow();

    }
  }
}
