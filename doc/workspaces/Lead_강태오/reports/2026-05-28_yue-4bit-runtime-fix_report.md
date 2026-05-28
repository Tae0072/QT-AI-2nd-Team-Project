
## 보안 및 저작권 메모

### CVE-2025-32434 우회 관련
pytorch_model.bin → model.safetensors 변환 시 	orch.load(weights_only=False)를 사용했습니다.
- **신뢰 출처 한정**: 대상 파일은 HuggingFace 공식 저장소(m-a-p/xcodec_mini_infer)에서 받은 파일로 출처가 명확합니다.
- **로컬 1회성 실행**: 네트워크 노출 없이 로컬 환경에서만 1회 실행 후 .safetensors로 대체합니다.
- **이후 운영**: 변환 완료 후 transformers는 .safetensors를 우선 로드하므로 	orch.load 경로가 더 이상 호출되지 않습니다.

### 저작권 리스크
- **YuE 모델**: Apache 2.0 라이선스로 배포. 상업적 사용 가능, 저작자 표기 권장.
- **생성 음원**: 사용자 제공 가사 기반으로 생성되며, 학습 데이터와의 유사성 검토는 사용자 책임입니다.
- **본 프로젝트 범위**: 개인 R&D 실험 목적으로만 사용. 제품 서비스에 음원을 배포하지 않습니다.