#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <poll.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <net/if.h>
#include <net/bpf.h>

#define MAX_FRAME 1518
#define BPF_BUFFER_SIZE 4096
#define IO_BUFFER_SIZE 4096

static int write_all(int fd,
                     const unsigned char *data,
                     size_t length)
{
    size_t offset = 0;

    while (offset < length) {
        ssize_t n = write(fd,
                          data + offset,
                          length - offset);

        if (n < 0) {
            if (errno == EINTR) {
                continue;
            }

            return -1;
        }

        if (n == 0) {
            return -1;
        }

        offset += (size_t)n;
    }

    return 0;
}

static int open_bpf(const char *interface)
{
    char name[32];
    struct ifreq ifr;

    int fd = -1;
    int dlt;

    u_int enable = 1;
    u_int disable = 0;
    u_int buffer_length = BPF_BUFFER_SIZE;

    for (int i = 0; i < 256; i++) {

        snprintf(name,
                 sizeof(name),
                 "/dev/bpf%d",
                 i);

        fd = open(name, O_RDWR);

        if (fd >= 0) {
            break;
        }

        if (errno != EBUSY) {
            return -1;
        }
    }

    if (fd < 0) {
        return -1;
    }

    /*
     * En macOS es importante configurar el buffer
     * antes de BIOCSETIF.
     */
    if (ioctl(fd,
              BIOCSBLEN,
              &buffer_length) < 0) {

        perror("BIOCSBLEN");
        close(fd);
        return -1;
    }

    fprintf(stderr,
            "BPF buffer configurado=%u\n",
            buffer_length);

    memset(&ifr, 0, sizeof(ifr));

    strncpy(ifr.ifr_name,
            interface,
            sizeof(ifr.ifr_name) - 1);

    if (ioctl(fd,
              BIOCSETIF,
              &ifr) < 0) {

        perror("BIOCSETIF");
        close(fd);
        return -1;
    }

    if (ioctl(fd,
              BIOCGBLEN,
              &buffer_length) < 0) {

        perror("BIOCGBLEN");
        close(fd);
        return -1;
    }

    fprintf(stderr,
            "BPF buffer real=%u\n",
            buffer_length);

    if (ioctl(fd,
              BIOCGDLT,
              &dlt) < 0) {

        perror("BIOCGDLT");
        close(fd);
        return -1;
    }

    fprintf(stderr,
            "BPF DLT=%d\n",
            dlt);

    /*
     * Entrega los paquetes inmediatamente.
     */
    if (ioctl(fd,
              BIOCIMMEDIATE,
              &enable) < 0) {

        perror("BIOCIMMEDIATE");
        close(fd);
        return -1;
    }

    /*
     * No queremos recibir nuevamente los frames
     * que nosotros mismos transmitimos.
     */
    if (ioctl(fd,
              BIOCSSEESENT,
              &disable) < 0) {

        perror("BIOCSSEESENT");
        close(fd);
        return -1;
    }

    /*
     * Permite recibir tráfico Ethernet que no esté
     * destinado directamente al host.
     */
    if (ioctl(fd,
              BIOCPROMISC,
              &enable) < 0) {

        perror("BIOCPROMISC");
        close(fd);
        return -1;
    }

    return fd;
}

static int send_frame(int fd,
                      const unsigned char *frame,
                      size_t length)
{
    if (length < 14 ||
        length > MAX_FRAME) {

        fprintf(stderr,
                "TX: longitud inválida: %zu\n",
                length);

        return -1;
    }

    ssize_t n =
        write(fd,
              frame,
              length);

    if (n < 0) {
        perror("BPF write");
        return -1;
    }

    if ((size_t)n != length) {

        fprintf(stderr,
                "BPF write incompleto: %zd/%zu\n",
                n,
                length);

        return -1;
    }

    return 0;
}

static int emit_frame(const unsigned char *frame,
                      size_t length)
{
    if (length == 0 ||
        length > MAX_FRAME) {

        return -1;
    }

    /*
     * Protocolo:
     *
     *   2 bytes: longitud big-endian
     *   N bytes: Ethernet frame
     */
    unsigned char header[2];

    header[0] =
        (unsigned char)((length >> 8) & 0xFF);

    header[1] =
        (unsigned char)(length & 0xFF);

    if (write_all(STDOUT_FILENO,
                  header,
                  sizeof(header)) < 0) {

        return -1;
    }

    if (write_all(STDOUT_FILENO,
                  frame,
                  length) < 0) {

        return -1;
    }

    return 0;
}

static int process_bpf_buffer(unsigned char *buffer,
                              size_t length)
{
    size_t offset = 0;

    while (offset < length) {

        /*
         * bpf_hdr mínimo válido en macOS:
         * 18 bytes.
         */
        if (length - offset < 18) {

            fprintf(stderr,
                    "BPF: encabezado incompleto\n");

            return -1;
        }

        struct bpf_hdr *hdr =
            (struct bpf_hdr *)(buffer + offset);

        if (hdr->bh_hdrlen < 18) {

            fprintf(stderr,
                    "BPF: bh_hdrlen inválido: %u\n",
                    hdr->bh_hdrlen);

            return -1;
        }

        if (hdr->bh_caplen == 0) {

            fprintf(stderr,
                    "BPF: bh_caplen=0\n");

            return -1;
        }

        if (hdr->bh_caplen > MAX_FRAME) {

            fprintf(stderr,
                    "BPF: frame demasiado grande: %u\n",
                    hdr->bh_caplen);

            return -1;
        }

        size_t frame_start =
            offset + hdr->bh_hdrlen;

        size_t frame_end =
            frame_start + hdr->bh_caplen;

        if (frame_end > length) {

            fprintf(stderr,
                    "BPF: frame fuera del buffer\n");

            return -1;
        }

        if (emit_frame(
                buffer + frame_start,
                hdr->bh_caplen) < 0) {

            return -1;
        }

        /*
         * Los registros BPF están alineados con
         * BPF_WORDALIGN().
         */
        offset +=
            BPF_WORDALIGN(
                hdr->bh_hdrlen +
                hdr->bh_caplen
            );
    }

    return 0;
}

int main(int argc, char **argv)
{
    if (argc != 2) {

        fprintf(stderr,
                "usage: %s <interface>\n",
                argv[0]);

        return 1;
    }

    /*
     * stdout contiene exclusivamente el protocolo
     * binario hacia Java.
     *
     * Todos los mensajes de diagnóstico van por stderr.
     */
    setvbuf(stdout,
            NULL,
            _IONBF,
            0);

    int bpf_fd =
        open_bpf(argv[1]);

    if (bpf_fd < 0) {

        perror("BPF");
        return 1;
    }

    fprintf(stderr,
            "BPF abierto en %s\n",
            argv[1]);

    unsigned char bpf_buffer[BPF_BUFFER_SIZE];
    unsigned char input_buffer[IO_BUFFER_SIZE];

    size_t input_length = 0;

    struct pollfd fds[2];

    fds[0].fd = bpf_fd;
    fds[0].events = POLLIN;

    fds[1].fd = STDIN_FILENO;
    fds[1].events = POLLIN;

    /*
     * Bucle permanente.
     *
     * RX:
     *   en0 -> BPF -> stdout -> Java
     *
     * TX:
     *   Java -> stdin -> BPF -> en0
     */
    for (;;) {

        int result =
            poll(fds,
                 2,
                 -1);

        if (result < 0) {

            if (errno == EINTR) {
                continue;
            }

            perror("poll");
            break;
        }

        /*
         * ============================
         * RX: Ethernet -> Java
         * ============================
         */
        if (fds[0].revents & POLLIN) {

            ssize_t n =
                read(bpf_fd,
                     bpf_buffer,
                     sizeof(bpf_buffer));

            if (n < 0) {

                if (errno == EINTR) {
                    continue;
                }

                perror("BPF read");
                break;
            }

            if (n > 0) {

                if (process_bpf_buffer(
                        bpf_buffer,
                        (size_t)n) < 0) {

                    break;
                }
            }
        }

        /*
         * ============================
         * TX: Java -> Ethernet
         * ============================
         */
        if (fds[1].revents & POLLIN) {

            ssize_t n =
                read(STDIN_FILENO,
                     input_buffer + input_length,
                     sizeof(input_buffer) -
                     input_length);

            if (n < 0) {

                if (errno == EINTR) {
                    continue;
                }

                perror("stdin");
                break;
            }

            if (n == 0) {
                break;
            }

            input_length +=
                (size_t)n;

            /*
             * Puede haber uno o varios frames
             * completos dentro del buffer.
             */
            while (input_length >= 2) {

                size_t frame_length =
                    ((size_t)input_buffer[0] << 8) |
                    (size_t)input_buffer[1];

                if (frame_length < 14 ||
                    frame_length > MAX_FRAME) {

                    fprintf(stderr,
                            "Entrada: longitud inválida: %zu\n",
                            frame_length);

                    close(bpf_fd);
                    return 1;
                }

                size_t total =
                    2 + frame_length;

                if (input_length < total) {
                    break;
                }

                if (send_frame(
                        bpf_fd,
                        input_buffer + 2,
                        frame_length) < 0) {

                    close(bpf_fd);
                    return 1;
                }

                memmove(
                    input_buffer,
                    input_buffer + total,
                    input_length - total
                );

                input_length -= total;
            }
        }

        if (fds[0].revents &
            (POLLERR | POLLHUP | POLLNVAL)) {

            break;
        }

        if (fds[1].revents &
            (POLLERR | POLLHUP | POLLNVAL)) {

            break;
        }
    }

    close(bpf_fd);

    return 0;
}
