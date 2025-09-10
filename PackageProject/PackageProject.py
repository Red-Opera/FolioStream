import os
from pathlib import Path

# 분할할 파일들 (현재 스크립트 기준 상대 경로)
FILES = [
    #'src/main/resources/static/images/Gallery/K-Project/banner.mp4',
    #'src/main/resources/static/images/Gallery/Legacy-of-Auras/banner.mp4',
    #'src/main/resources/static/images/Gallery/Era-of-Dreams/banner.mp4',
]

# 청크 크기: 95 MiB
CHUNK_SIZE = 95 * 1024 * 1024  # 95MB

def split_file(file_path: Path, chunk_size: int = CHUNK_SIZE) -> None:
    """
    지정된 파일을 chunk_size 바이트 단위로 분할하여
    같은 디렉터리에 .partNN 형식으로 저장하고,
    완료 후 원본 파일은 삭제합니다.
    """
    if not file_path.exists():
        print(f"❌ 파일을 찾을 수 없습니다: {file_path}")
        return

    file_size = file_path.stat().st_size
    if file_size <= chunk_size:
        print(f"ℹ️ {file_path.name} (크기: {file_size}바이트)은 분할 불필요")
        return

    with file_path.open('rb') as src:
        part = 1
        while True:
            data = src.read(chunk_size)
            if not data:
                break
            out_name = file_path.with_name(f"{file_path.name}.part{part:02d}")
            with out_name.open('wb') as dst:
                dst.write(data)
            print(f"✅ 생성: {out_name.name} ({len(data)}바이트)")
            part += 1

    # 분할 완료 메시지
    print(f"🎉 완료: {file_path.name} → {part-1}개 파트 분할")

    # 원본 파일 삭제
    try:
        file_path.unlink()
        print(f"🗑️ 원본 파일 삭제: {file_path.name}")
    except Exception as e:
        print(f"⚠️ 원본 파일 삭제 중 오류 발생: {e}")

if __name__ == '__main__':
    # script 파일의 위치에서 부모 디렉터리를 세 번 거슬러 올라갑니다.
    base_dir = Path(__file__).parent.parent

    for rel in FILES:
        full = (base_dir / rel).resolve()
        split_file(full)
