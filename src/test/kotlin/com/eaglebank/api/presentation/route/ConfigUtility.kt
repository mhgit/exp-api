package com.eaglebank.api.util

import com.typesafe.config.Config

fun withConfig(config: Config, test: (Config) -> Config): Config {
    return test(config)
}