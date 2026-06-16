import 'package:flutter/material.dart';

import 'sharing_feed_palette.dart';

/// 목차 번호 페이저. 한 번에 **5개씩** 번호를 보여주고(블록 단위), 양옆 화살표로 이동한다.
///
/// - [currentPage] : 현재 페이지(0부터)
/// - [totalPages]  : 전체 페이지 수
/// - [onSelect]    : 번호/화살표를 누르면 이동할 페이지(0부터)를 전달
///
/// 전체가 1페이지 이하면 아무것도 그리지 않는다.
class PageNavigator extends StatelessWidget {
  final int currentPage;
  final int totalPages;
  final ValueChanged<int> onSelect;

  /// 한 블록에 보이는 번호 개수(요구사항: 5개).
  static const int blockSize = 5;

  const PageNavigator({
    super.key,
    required this.currentPage,
    required this.totalPages,
    required this.onSelect,
  });

  @override
  Widget build(BuildContext context) {
    if (totalPages <= 1) return const SizedBox.shrink();

    // 현재 페이지가 속한 5개 블록의 시작·끝(0부터).
    final blockStart = (currentPage ~/ blockSize) * blockSize;
    final blockEnd =
        (blockStart + blockSize) > totalPages ? totalPages : blockStart + blockSize;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          // 이전: 현재 페이지가 0이면 비활성.
          _arrow(Icons.chevron_left,
              enabled: currentPage > 0,
              onTap: () => onSelect(currentPage - 1)),
          for (int p = blockStart; p < blockEnd; p++)
            _numberButton(p, selected: p == currentPage),
          // 다음: 마지막 페이지면 비활성.
          _arrow(Icons.chevron_right,
              enabled: currentPage < totalPages - 1,
              onTap: () => onSelect(currentPage + 1)),
        ],
      ),
    );
  }

  Widget _numberButton(int page, {required bool selected}) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 3),
      child: InkWell(
        onTap: selected ? null : () => onSelect(page),
        borderRadius: BorderRadius.circular(8),
        child: Container(
          width: 34,
          height: 34,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: selected ? SharingFeedPalette.text : Colors.transparent,
            borderRadius: BorderRadius.circular(8),
            border: selected
                ? null
                : Border.all(color: SharingFeedPalette.chipBorder),
          ),
          child: Text(
            '${page + 1}',
            style: TextStyle(
              fontFamily: 'GowunDodum',
              fontSize: 14,
              fontWeight: selected ? FontWeight.w700 : FontWeight.w400,
              color: selected ? Colors.white : SharingFeedPalette.muted,
            ),
          ),
        ),
      ),
    );
  }

  Widget _arrow(IconData icon,
      {required bool enabled, required VoidCallback onTap}) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 2),
      child: InkWell(
        onTap: enabled ? onTap : null,
        borderRadius: BorderRadius.circular(8),
        child: SizedBox(
          width: 34,
          height: 34,
          child: Icon(
            icon,
            size: 22,
            color: enabled ? SharingFeedPalette.text : SharingFeedPalette.chipBorder,
          ),
        ),
      ),
    );
  }
}
