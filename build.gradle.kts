// 루트 빌드 파일. 공통 설정은 두지 않고 각 서브프로젝트가 자기 plugin을 선언한다.
// 이렇게 두면 v2.0 Modular Monolith로 합쳐질 때 본 파일이 자연스럽게 단일 build.gradle.kts가 된다.

allprojects {
    repositories {
        mavenCentral()
    }
}
