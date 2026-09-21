package com.funasr.sv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({SvProperties.class, AsrProperties.class})
public class SvApplication {
  public static void main(String[] args) {
    SpringApplication.run(SvApplication.class, args);
  }
}
