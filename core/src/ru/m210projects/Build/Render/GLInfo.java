/*
 * GLInfo for "POLYMOST" code written by Ken Silverman
 * Ken Silverman's official web site: http://www.advsys.net/ken
 *
 * See the included license file "BUILDLIC.TXT" for license info.
 */

package ru.m210projects.Build.Render;

import static com.badlogic.gdx.graphics.GL20.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.badlogic.gdx.Gdx;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.BufferUtils;
import ru.m210projects.Build.Render.Types.GL10;

public class GLInfo {
	public static String version;

	public static float maxanisotropy;
	public static boolean bgra;
//	public static boolean clamptoedge;
	public static byte texcompr;
	public static byte texnpot;
	public static byte multisample;
	public static byte nvmultisamplehint;
	public static byte multitex;
	public static byte envcombine;
	public static byte vbos;
	public static boolean hack_nofog;
	public static boolean supportsGenerateMipmaps;
	public static int gltexmaxsize;

	public static void init(GL20 gl) {
		gl.glEnable(GL_TEXTURE_2D);
		gl.glClearColor(0, 0, 0, 0.5f); // Black Background
		gl.glDisable(GL_DITHER);

		version = gl.glGetString(GL_VERSION);

		maxanisotropy = 1.0f;
		bgra = false;
		texcompr = 0;
		gltexmaxsize = 0;

		if (Gdx.graphics.supportsExtension("GL_EXT_texture_filter_anisotropic")) {
			FloatBuffer buf = BufferUtils.newFloatBuffer(16);
			gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, buf);
			maxanisotropy = buf.get(); // supports anisotropy. get the maximum anisotropy level
		}
//		if (Gdx.graphics.supportsExtension("GL_EXT_texture_edge_clamp") ||
//				Gdx.graphics.supportsExtension("GL_SGIS_texture_edge_clamp")) {
//			clamptoedge = true; // supports GL_CLAMP_TO_EDGE or GL_CLAMP_TO_EDGE_SGIS
//		}
		if (Gdx.graphics.supportsExtension("GL_EXT_bgra")) {
			bgra = true; // support bgra textures
		}
		if (Gdx.graphics.supportsExtension("GL_ARB_texture_compression")) {
			texcompr = 1; // support texture compression
		}
		if (Gdx.graphics.supportsExtension("GL_ARB_texture_non_power_of_two")) {
//			texnpot = 1; // support non-power-of-two texture sizes
		}
		if (Gdx.graphics.supportsExtension("WGL_3DFX_gamma_control")) {
			hack_nofog = true; // 3dfx cards have issues with fog
		}
		if (Gdx.graphics.supportsExtension("GL_ARB_multisample")) {
			multisample = 1; // supports multisampling
		}
		if (Gdx.graphics.supportsExtension("GL_NV_multisample_filter_hint")) {
			nvmultisamplehint = 1; // supports nvidia's multisample hint extension
		}
		if (Gdx.graphics.supportsExtension("GL_ARB_multitexture")) {
			multitex = 1;
		}
		if (Gdx.graphics.supportsExtension("GL_ARB_texture_env_combine")) {
			envcombine = 1;
		}
		if (Gdx.graphics.supportsExtension("GL_ARB_vertex_buffer_object")) {
			vbos = 1;
		}
		if (Gdx.graphics.supportsExtension("GL_ARB_framebuffer_object")
				|| Gdx.graphics.supportsExtension("GL_EXT_framebuffer_object") || Gdx.gl30 != null
				|| Gdx.app.getType() == ApplicationType.Android || Gdx.app.getType() == ApplicationType.WebGL
				|| Gdx.app.getType() == ApplicationType.iOS) {
			supportsGenerateMipmaps = true;
		}

		IntBuffer textureSize = BufferUtils.newIntBuffer(16);
		gl.glGetIntegerv(GL_MAX_TEXTURE_SIZE, textureSize);
		int i = textureSize.get();
		if (i == 0) {
			gltexmaxsize = 6; // 2^6 = 64 == default GL max texture size
		} else {
			gltexmaxsize = 0;
			for (; i > 1; i >>= 1) {
				gltexmaxsize++;
			}
		}
	}

	public static float getMaxAnisotropicFilterLevel() {
		return maxanisotropy;
	}

	public static int calcSize(int size) {
		int nsize = 1;
		if (GLInfo.texnpot == 0) {
			for (; nsize < size; nsize *= 2) {
				;
			}
			return nsize;
		}
		return size == 0 ? 1 : size;
	}
}
