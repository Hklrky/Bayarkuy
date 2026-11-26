#!/bin/bash

# ========================================================
# KONFIGURASI WARNA TAMPILAN TERMUX
# ========================================================
HIJAU='\033[1;32m'
KUNING='\033[1;33m'
BIRU='\033[1;36m'
UNGU='\033[1;35m'
MERAH='\033[1;31m'
PUTIH='\033[1;37m'
ABU='\033[0;37m'
RESET='\033[0m'

CONFIG_FILE=".git_manager_config"

# Muat konfigurasi jika ada, atau gunakan default
if [ -f "$CONFIG_FILE" ]; then
    source "$CONFIG_FILE"
else
    REPO_URL="https://github.com/Hklrky/Absensi-QR.git"
    BRANCH_UTAMA="main"
    GIT_USER_NAME="Hklrky"
    GIT_USER_EMAIL="hklrky@users.noreply.github.com"
fi

simpan_config() {
    cat <<EOF > "$CONFIG_FILE"
REPO_URL="$REPO_URL"
BRANCH_UTAMA="$BRANCH_UTAMA"
GIT_USER_NAME="$GIT_USER_NAME"
GIT_USER_EMAIL="$GIT_USER_EMAIL"
EOF
}

# Pengaturan aman untuk Termux & Git
git config --global --add safe.directory "$(pwd)" 2>/dev/null
git config --global credential.helper store
git config --global user.name "$GIT_USER_NAME"
git config --global user.email "$GIT_USER_EMAIL"

# Inisialisasi git otomatis jika belum ada
if [ ! -d ".git" ]; then
    git init -q
    git branch -M "$BRANCH_UTAMA" 2>/dev/null
    git remote add origin "$REPO_URL" 2>/dev/null
fi

if [ -z "$(git remote get-url origin 2>/dev/null)" ]; then
    git remote add origin "$REPO_URL"
fi

jeda() {
    echo -e "\n${KUNING}Tekan [ENTER] untuk kembali ke menu utama...${RESET}"
    read -r
}

# ========================================================
# MENU UTAMA INTERAKTIF
# ========================================================
while true; do
    clear
    JUMLAH_UBAH=$(git status -s | wc -l)
    BRANCH_AKTIF=$(git branch --show-current 2>/dev/null || echo "$BRANCH_UTAMA")

    echo -e "${KUNING}========================================================${RESET}"
    echo -e "${BIRU}   ⚡ GIT MANAGER PRO - ABSENSI QR COMIC ⚡          ${RESET}"
    echo -e "${KUNING}========================================================${RESET}"
    echo -e "${ABU}Folder : ${PUTIH}$(basename "$(pwd)")${RESET}  |  ${ABU}Branch : ${HIJAU}$BRANCH_AKTIF${RESET}"
    echo -e "${ABU}Status : ${KUNING}$JUMLAH_UBAH file lokal belum di-push${RESET}"
    echo -e "${KUNING}--------------------------------------------------------${RESET}"
    echo -e "${HIJAU}[1]${RESET} 🔄 Sinkronisasi Aman (Pull & Push Tanpa Hapus Data)"
    echo -e "${HIJAU}[2]${RESET} 🚀 Push Normal (Kirim perubahan lokal)"
    echo -e "${KUNING}[3]${RESET} ⚡ Force Push (Paksa Timpa jika Bentrok/Rejected)"
    echo -e "${UNGU}[4]${RESET} ⚙️  Menu Konfigurasi (Ubah URL/Branch/User)"
    echo -e "${MERAH}[5]${RESET} 🧹 Reset Total Git (Mulai dari Nol, File Aman)"
    echo -e "${BIRU}[6]${RESET} 📋 Cek Status Perubahan File"
    echo -e "${PUTIH}[7]${RESET} 📜 Lihat Riwayat 5 Commit Terakhir"
    echo -e "${MERAH}[8]${RESET} 🔑 Hapus/Reset Token GitHub"
    echo -e "${ABU}[0]${RESET} ❌ Keluar"
    echo -e "${KUNING}--------------------------------------------------------${RESET}"
    read -p "Pilih menu [0-8]: " PILIHAN

    case $PILIHAN in
        1)
            echo -e "\n${BIRU}=== 🔄 SINKRONISASI AMAN (PULL & PUSH) ===${RESET}"
            echo -e "${PUTIH}Langkah 1: Mengambil update dari GitHub (agar file lama aman)...${RESET}"
            
            if git pull origin "$BRANCH_UTAMA" --allow-unrelated-histories --no-rebase; then
                echo -e "\n${HIJAU}[✓] Sinkronisasi Berhasil! File GitHub dan HP digabungkan.${RESET}"
            else
                echo -e "\n${MERAH}[!] Terdeteksi konflik pada baris file yang sama.${RESET}"
                echo -e "${KUNING}Periksa file yang bentrok, perbaiki, lalu ulangi menu ini.${RESET}"
                jeda
                continue
            fi

            if [[ ! -z $(git status -s) ]]; then
                echo -e "\n${PUTIH}Langkah 2: Mengirim gabungan file terbaru ke GitHub...${RESET}"
                git add .
                git commit -m "Auto-sync and update Absensi QR ($(date +"%d-%m-%Y %H:%M"))" 2>/dev/null
                if git push -u origin "$BRANCH_UTAMA"; then
                    echo -e "\n${HIJAU}🎉 BERHASIL! Data di GitHub aman dan kode terbaru terkirim!${RESET}"
                else
                    echo -e "\n${MERAH}⛔ Gagal melakukan push setelah sinkronisasi.${RESET}"
                fi
            else
                echo -e "\n${HIJAU}✨ Semua data sudah sinkron sempurna! Tidak ada yang perlu di-push.${RESET}"
            fi
            jeda
            ;;

        2)
            echo -e "\n${BIRU}=== 🚀 PUSH MANUAL ===${RESET}"
            if [[ -z $(git status -s) ]]; then
                echo -e "${MERAH}[!] Tidak ada perubahan file baru untuk di-push.${RESET}"
                jeda
                continue
            fi
            git add .
            read -p "Tulis pesan commit (ENTER untuk otomatis): " PESAN
            [ -z "$PESAN" ] && PESAN="Update Absensi QR ($(date +"%d-%m-%Y %H:%M"))"
            git commit -m "$PESAN"
            
            if git push -u origin "$BRANCH_UTAMA"; then
                echo -e "\n${HIJAU}🎉 Push Berhasil!${RESET}"
            else
                echo -e "\n${MERAH}⛔ Push Ditolak! Gunakan Menu [1] (Sinkronisasi Aman) agar file di GitHub tidak tertimpa.${RESET}"
            fi
            jeda
            ;;

        3)
            echo -e "\n${KUNING}=== ⚡ FORCE PUSH (PAKSA TIMPA) ===${RESET}"
            echo -e "${MERAH}Peringatan: Ini memaksa isi folder HP menimpa total riwayat di GitHub.${RESET}"
            read -p "Yakin ingin Force Push? (y/n): " YAKIN_F
            if [[ "$YAKIN_F" == "y" || "$YAKIN_F" == "Y" ]]; then
                git add .
                git commit -m "Force update Absensi QR" 2>/dev/null
                if git push -u origin "$BRANCH_UTAMA" --force; then
                    echo -e "\n${HIJAU}🎉 Force Push Berhasil!${RESET}"
                else
                    echo -e "\n${MERAH}⛔ Gagal Force Push.${RESET}"
                fi
            fi
            jeda
            ;;

        4)
            echo -e "\n${UNGU}=== ⚙️ MENU KONFIGURASI REPOSITORI ===${RESET}"
            echo -e "URL Repo Saat Ini : ${PUTIH}$REPO_URL${RESET}"
            echo -e "Branch Utama      : ${PUTIH}$BRANCH_UTAMA${RESET}"
            echo -e "Git Username      : ${PUTIH}$GIT_USER_NAME${RESET}"
            echo -e "----------------------------------------"
            echo -e "1. Ubah URL Repository"
            echo -e "2. Ubah Nama Branch (main / master)"
            echo -e "3. Ubah Nama & Email Git"
            echo -e "0. Kembali"
            read -p "Pilih opsi [0-3]: " OPSI_CFG

            if [ "$OPSI_CFG" == "1" ]; then
                read -p "Masukkan URL Repository baru: " BARU_URL
                [ ! -z "$BARU_URL" ] && REPO_URL="$BARU_URL" && git remote set-url origin "$REPO_URL" 2>/dev/null && simpan_config && echo -e "${HIJAU}[✓] URL Diperbarui!${RESET}"
            elif [ "$OPSI_CFG" == "2" ]; then
                read -p "Masukkan nama branch baru: " BARU_B
                [ ! -z "$BARU_B" ] && BRANCH_UTAMA="$BARU_B" && git branch -M "$BRANCH_UTAMA" && simpan_config && echo -e "${HIJAU}[✓] Branch Diperbarui!${RESET}"
            elif [ "$OPSI_CFG" == "3" ]; then
                read -p "Username baru: " B_USER
                read -p "Email baru: " B_MAIL
                [ ! -z "$B_USER" ] && [ ! -z "$B_MAIL" ] && GIT_USER_NAME="$B_USER" && GIT_USER_EMAIL="$B_MAIL" && simpan_config && echo -e "${HIJAU}[✓] Identitas Diperbarui!${RESET}"
            fi
            jeda
            ;;

        5)
            echo -e "\n${MERAH}=== 🧹 RESET TOTAL GIT (BERSIHKAN RIWAYAT) ===${RESET}"
            echo -e "${KUNING}Fitur ini membersihkan riwayat commit lama. File kodinganmu di HP 100% aman.${RESET}"
            read -p "Yakin ingin reset total? (y/n): " YAKIN_R
            if [[ "$YAKIN_R" == "y" || "$YAKIN_R" == "Y" ]]; then
                rm -rf .git
                git init -q
                git branch -M "$BRANCH_UTAMA"
                git remote add origin "$REPO_URL"
                git add .
                git commit -m "Fresh start: Reset Git history"
                if git push -u origin "$BRANCH_UTAMA" --force; then
                    echo -e "\n${HIJAU}🎉 Reset Berhasil! Riwayat dibersihkan dan di-push ulang dari nol.${RESET}"
                else
                    echo -e "\n${MERAH}⛔ Gagal melakukan push setelah reset.${RESET}"
                fi
            fi
            jeda
            ;;

        6)
            echo -e "\n${BIRU}=== 📋 STATUS PERUBAHAN FILE ===${RESET}"
            git status
            jeda
            ;;

        7)
            echo -e "\n${BIRU}=== 📜 RIWAYAT COMMIT ===${RESET}"
            git log -n 5 --oneline
            jeda
            ;;

        8)
            echo -e "\n${MERAH}=== 🔑 RESET TOKEN GITHUB ===${RESET}"
            rm -f ~/.git-credentials
            git config --global credential.helper store
            echo -e "${HIJAU}[✓] Token lama dihapus. Input token baru saat push berikutnya.${RESET}"
            jeda
            ;;

        0)
            echo -e "\n${HIJAU}Sampai jumpa! Semangat terus ngodingnya! 🚀${RESET}"
            exit 0
            ;;

        *)
            echo -e "\n${MERAH}Pilihan tidak valid!${RESET}"
            sleep 1
            ;;
    esac
done