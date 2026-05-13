# QT-AI 프로젝트 개요

QT-AI는 성경 QT(조용한 시간)를 돕는 AI 코칭 앱입니다.

## 기술 스택

Flutter 3.24 + Spring Boot 3.3 / Java 21, MSA 4서비스(Gateway, BFF, Bible, AI) + Kafka

## 팀원

- 강태오 (Lead) — Gateway, BFF, DevOps, AI 보조
- 이지윤 — Bible Service
- 김태혁 — AI/RAG Service
- 강상민 — AI/RAG Service
- 이승욱 — Bible Service Journal/Kafka
- 김지민 — Flutter 앱

> 2026-05-12 결정 기준: 독립 Auth Service와 Journal Service는 만들지 않습니다. Auth는 Gateway Auth 모듈, Journal은 Bible Service 내부 도메인입니다.
