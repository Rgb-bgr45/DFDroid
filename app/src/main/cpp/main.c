#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include "SDL.h"
#include "SDL_main.h"

extern char **environ;
extern int box64_main(int argc, char **argv, char **env);

int SDL_main(int argc, char **argv) {
    const char *dir = getenv("DF_GAME_DIR");
    const char *log = getenv("DF_LOG");
    if (log) {
        int fd = open(log, O_WRONLY | O_CREAT | O_APPEND, 0644);
        if (fd >= 0) { dup2(fd, 1); dup2(fd, 2); close(fd); }
        setvbuf(stdout, NULL, _IONBF, 0);
        setvbuf(stderr, NULL, _IONBF, 0);
    }
    if (dir && chdir(dir) != 0) { fprintf(stderr, "[DFDroid] chdir failed: %s\n", dir); return 1; }
    fprintf(stderr, "[DFDroid] box64 basliyor, cwd=%s\n", dir ? dir : "?");
    char *args[] = { "box64", "./libs/Dwarf_Fortress", NULL };
    int r = box64_main(2, args, environ);
    fprintf(stderr, "[DFDroid] box64 cikti, kod=%d\n", r);
    return r;
}
