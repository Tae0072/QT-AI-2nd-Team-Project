import 'package:flutter/material.dart';

import '../theme/app_theme.dart';

/// Calm Paper 공통 컴포넌트 (DESIGN_PROTOTYPE.md의 .group / .row / .section-t / .badge 대응).
///
/// 흰색 Material Card 대신 **납작한 sunken 그룹 박스 + hairline 구분선**을 쓴다.
/// 화면 코드는 색을 하드코딩하지 않고 이 위젯 + `context.appColors` 토큰만 사용한다.

/// 섹션 소제목 (.section-t): 12 w600 textMuted.
class CpSectionTitle extends StatelessWidget {
  final String text;
  const CpSectionTitle(this.text, {super.key});

  @override
  Widget build(BuildContext context) {
    final c = context.appColors;
    return Padding(
      padding: const EdgeInsets.fromLTRB(2, 20, 2, 8),
      child: Text(
        text,
        style: TextStyle(
          fontFamily: 'GowunDodum',
          fontSize: 12,
          fontWeight: FontWeight.w600,
          color: c.textMuted,
          letterSpacing: 0.3,
        ),
      ),
    );
  }
}

/// 타이틀 블록 (.tblock): (이모지+)대제목 26 w700 + 한글 부제 + 영문 부제.
class CpTitleBlock extends StatelessWidget {
  final String title;
  final String? emoji;
  final String? sub;
  final String? subEn;
  const CpTitleBlock(
      {super.key, required this.title, this.emoji, this.sub, this.subEn});

  @override
  Widget build(BuildContext context) {
    final c = context.appColors;
    return Padding(
      padding: const EdgeInsets.fromLTRB(2, 6, 2, 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            emoji == null ? title : '$emoji  $title',
            style: TextStyle(
              fontFamily: 'GowunDodum',
              fontSize: 26,
              fontWeight: FontWeight.w700,
              letterSpacing: -0.5,
              height: 1.3,
              color: c.text,
            ),
          ),
          if (sub != null) ...[
            const SizedBox(height: 6),
            Text(sub!,
                style: TextStyle(
                    fontFamily: 'GowunDodum', fontSize: 13, color: c.text2)),
          ],
          if (subEn != null) ...[
            const SizedBox(height: 2),
            Text(subEn!,
                style: TextStyle(
                    fontFamily: 'GowunDodum',
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                    color: c.textMuted)),
          ],
        ],
      ),
    );
  }
}

/// 그룹 박스 (.group): sunken 배경 + 반경 14, 내부 행은 hairline으로 구분.
class CpGroup extends StatelessWidget {
  final List<Widget> children;
  final EdgeInsetsGeometry margin;
  const CpGroup(
      {super.key,
      required this.children,
      this.margin = const EdgeInsets.only(bottom: 16)});

  @override
  Widget build(BuildContext context) {
    final c = context.appColors;
    final rows = <Widget>[];
    for (var i = 0; i < children.length; i++) {
      if (i > 0) {
        rows.add(Divider(
            height: 1, thickness: 1, color: c.hairline, indent: 16, endIndent: 16));
      }
      rows.add(children[i]);
    }
    return Container(
      margin: margin,
      decoration: BoxDecoration(
        color: c.bgSunken,
        borderRadius: BorderRadius.circular(14),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(mainAxisSize: MainAxisSize.min, children: rows),
    );
  }
}

/// 그룹 내 한 행 (.row): (leading 아이콘 +) 제목 + (meta/배지 +) (쉐브론).
class CpRow extends StatelessWidget {
  final IconData? leading;
  final String title;
  final String? meta;
  final Widget? trailing;
  final bool chevron;
  final VoidCallback? onTap;
  final Color? titleColor;
  final FontWeight titleWeight;
  const CpRow({
    super.key,
    this.leading,
    required this.title,
    this.meta,
    this.trailing,
    this.chevron = false,
    this.onTap,
    this.titleColor,
    this.titleWeight = FontWeight.w400,
  });

  @override
  Widget build(BuildContext context) {
    final c = context.appColors;
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 15),
        child: Row(
          children: [
            if (leading != null) ...[
              Icon(leading, size: 20, color: c.text),
              const SizedBox(width: 12),
            ],
            Expanded(
              child: Text(
                title,
                style: TextStyle(
                    fontFamily: 'GowunDodum',
                    fontSize: 15,
                    fontWeight: titleWeight,
                    color: titleColor ?? c.text),
              ),
            ),
            if (meta != null)
              Padding(
                padding: const EdgeInsets.only(left: 8),
                child: Text(meta!,
                    style: TextStyle(
                        fontFamily: 'GowunDodum', fontSize: 13, color: c.text2)),
              ),
            if (trailing != null)
              Padding(
                  padding: const EdgeInsets.only(left: 8), child: trailing!),
            if (chevron)
              Padding(
                padding: const EdgeInsets.only(left: 6),
                child: Icon(Icons.chevron_right, size: 20, color: c.textMuted),
              ),
          ],
        ),
      ),
    );
  }
}

/// 배지 (.badge): sunken 캡슐 + sec 텍스트. dot=true면 앞에 빨간 accentDot 점.
class CpBadge extends StatelessWidget {
  final String text;
  final bool dot;
  const CpBadge(this.text, {super.key, this.dot = false});

  @override
  Widget build(BuildContext context) {
    final c = context.appColors;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 3),
      decoration: BoxDecoration(
        color: c.bgElevated,
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: c.hairline),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (dot) ...[
            Container(
                width: 5,
                height: 5,
                decoration:
                    BoxDecoration(color: c.accentDot, shape: BoxShape.circle)),
            const SizedBox(width: 5),
          ],
          Text(text,
              style: TextStyle(
                  fontFamily: 'GowunDodum',
                  fontSize: 11,
                  fontWeight: FontWeight.w600,
                  color: c.text2)),
        ],
      ),
    );
  }
}

/// 보조 콘텐츠 박스 (.sub-box): sunken 배경, 반경 14, 패딩 14.
class CpSubBox extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry margin;
  const CpSubBox(
      {super.key, required this.child, this.margin = EdgeInsets.zero});

  @override
  Widget build(BuildContext context) {
    final c = context.appColors;
    return Container(
      margin: margin,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: c.bgSunken,
        borderRadius: BorderRadius.circular(14),
      ),
      child: child,
    );
  }
}
