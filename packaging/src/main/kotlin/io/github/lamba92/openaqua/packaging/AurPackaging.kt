package io.github.lamba92.openaqua.packaging

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.writeText

fun generateAurFiles(
    metadata: AppMetadata,
    outputDir: Path,
    version: String,
) {
    val aurDir = (outputDir / "aur").createDirectories()
    val aurFile = aurDir / "PKGBUILD"
    aurFile.writeText(generatePkgbuild(metadata, version))
}

fun generatePkgbuild(
    metadata: AppMetadata,
    version: String,
): String {
    val d = "$"
    return """
        |# Maintainer: ${metadata.developer.name} <${metadata.developer.email}>
        |pkgname=${metadata.executable}-bin
        |_pkgname=${metadata.executable}
        |pkgver=$version
        |pkgrel=1
        |pkgdesc="${metadata.summary}"
        |arch=('x86_64')
        |url="${metadata.project.homepage}"
        |license=('${metadata.project.license}')
        |depends=('gtk3' 'hicolor-icon-theme' 'alsa-lib')
        |provides=("$d{_pkgname}")
        |conflicts=("$d{_pkgname}")
        |source=("$d{_pkgname}-$d{pkgver}.tar.gz::https://github.com/${metadata.project.githubRepo}/releases/download/v$d{pkgver}/${metadata.executable}-$d{pkgver}-linux-x64.tar.gz"
        |        "${metadata.id}.desktop::https://raw.githubusercontent.com/${metadata.project.githubRepo}/v$d{pkgver}/packaging/metadata/${metadata.id}.desktop"
        |        "${metadata.iconName}.png::https://raw.githubusercontent.com/${metadata.project.githubRepo}/v$d{pkgver}/packaging/metadata/${metadata.iconName}.png"
        |        "${metadata.id}.metainfo.xml::https://raw.githubusercontent.com/${metadata.project.githubRepo}/v$d{pkgver}/packaging/metadata/${metadata.id}.metainfo.xml"
        |        "99-aquacomputer.rules::https://raw.githubusercontent.com/${metadata.project.githubRepo}/v$d{pkgver}/packaging/metadata/99-aquacomputer.rules")
        |sha256sums=('SKIP'
        |            'SKIP'
        |            'SKIP'
        |            'SKIP'
        |            'SKIP')
        |
        |package() {
        |    install -dm755 "$d{pkgdir}/opt/$d{_pkgname}"
        |    cp -dr --no-preserve=ownership "$d{srcdir}/$d{_pkgname}/"* "$d{pkgdir}/opt/$d{_pkgname}/"
        |
        |    install -dm755 "$d{pkgdir}/usr/bin"
        |    ln -s "/opt/$d{_pkgname}/bin/$d{_pkgname}" "$d{pkgdir}/usr/bin/$d{_pkgname}"
        |
        |    install -Dm644 "$d{srcdir}/${metadata.id}.desktop" "$d{pkgdir}/usr/share/applications/${metadata.id}.desktop"
        |    install -Dm644 "$d{srcdir}/${metadata.iconName}.png" "$d{pkgdir}/usr/share/icons/hicolor/512x512/apps/${metadata.iconName}.png"
        |    install -Dm644 "$d{srcdir}/${metadata.id}.metainfo.xml" "$d{pkgdir}/usr/share/metainfo/${metadata.id}.metainfo.xml"
        |    install -Dm644 "$d{srcdir}/99-aquacomputer.rules" "$d{pkgdir}/usr/lib/udev/rules.d/99-aquacomputer.rules"
        |}
        """.trimMargin() + "\n"
}
