# box64'u executable yerine SHARED kutuphane yapar ve main -> box64_main olarak yeniden adlandirir.
import re
p = 'app/third_party/box64/CMakeLists.txt'
s = open(p, encoding='utf-8').read()
n = re.sub(r'add_executable\(\s*\$\{BOX64\}', 'add_library(${BOX64} SHARED', s)
if n == s:
    print('UYARI: add_executable(${BOX64} ...) bulunamadi, box64 CMakeLists degismis olabilir!')
n += '\nset_source_files_properties(src/main.c PROPERTIES COMPILE_DEFINITIONS "main=box64_main")\n'
open(p, 'w', encoding='utf-8').write(n)
print('box64 yamalandi')
