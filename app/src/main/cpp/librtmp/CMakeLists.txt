
#定义宏    如果代码中定义了 #define NO_CRYPTO
#就表示不适用ssl, 不支持rtmps, 这里定义不支持ssl
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNO_CRYPTO")

aux_source_directory(. SOURCE)

add_library(rtmp STATIC ${SOURCE})