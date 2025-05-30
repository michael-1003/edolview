// Source from: https://github.com/kbinani/colormap-shaders
float colormap_red(float x) {
    if (x < (3.27037346938775E+02 + 9.33750000000000E+00) / (1.81250000000000E+01 + 5.18306122448980E+00)) { // 14.0428817217
        return 1.81250000000000E+01 * x - 9.33750000000000E+00;
    } else if (x <= 64.0) {
        return -5.18306122448980E+00 * x + 3.27037346938775E+02;
    } else {
        return x;
    }
}

float colormap_blue(float x) {
    if (x < (8.01533134203946E+02 + 1.96917113893858E+00) / (1.99964221824687E+00 + 4.25020839121978E+00)) { // 128.063441841
        return 1.99964221824687E+00 * x - 1.96917113893858E+00;
    } else if (x < (8.01533134203946E+02 + 7.17997825045893E+02) / (3.80632931598691E+00 + 4.25020839121978E+00)) {
        return -4.25020839121978E+00 * x + 8.01533134203946E+02;
    } else {
        return 3.80632931598691E+00 * x - 7.17997825045893E+02;
    }
}

vec3 IDL_Stern_Special_colormap(float x) {
    float t = x * 255.0;
    float r = clamp(colormap_red(t) / 255.0, 0.0, 1.0);
    float b = clamp(colormap_blue(t) / 255.0, 0.0, 1.0);
    return vec3(r, x, b);
}
