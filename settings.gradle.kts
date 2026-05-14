rootProject.name = "qtai-platform"

// 4 백엔드 서비스 멀티프로젝트 등록.
// v2.0 Modular Monolith 전환 시점에는 본 파일이 사라지고 단일 qtai-server 프로젝트가 된다.
include(":gateway")
include(":bible-service")
include(":ai-service")
include(":bff-aggregator")

project(":gateway").projectDir         = file("services/gateway")
project(":bible-service").projectDir   = file("services/bible-service")
project(":ai-service").projectDir      = file("services/ai-service")
project(":bff-aggregator").projectDir  = file("services/bff-aggregator")
