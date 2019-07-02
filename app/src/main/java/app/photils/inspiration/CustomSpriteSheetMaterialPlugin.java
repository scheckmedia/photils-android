package app.photils.inspiration;

import android.opengl.GLES20;
import android.os.SystemClock;

import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.plugins.IMaterialPlugin;
import org.rajawali3d.materials.shaders.AShader;
import org.rajawali3d.materials.shaders.IShaderFragment;

public class CustomSpriteSheetMaterialPlugin implements IMaterialPlugin {
    CustomSpriteSheetVertexShaderFragment mVertexShader;

    public CustomSpriteSheetMaterialPlugin(int numTilesX, int numTilesY, float fps,
                                           int numFrames, boolean isLoop)
    {
        mVertexShader = new CustomSpriteSheetVertexShaderFragment();
        mVertexShader.setNumTiles(numTilesX, numTilesY);
        mVertexShader.setFPS(fps);
        mVertexShader.setNumFrames(numFrames);
        mVertexShader.setLoop(isLoop);
    }

    public Material.PluginInsertLocation getInsertLocation() {
        return Material.PluginInsertLocation.PRE_LIGHTING;
    }

    public IShaderFragment getVertexShaderFragment() {
        return mVertexShader;
    }

    public IShaderFragment getFragmentShaderFragment() {
        return null;
    }

    public void play() {
        mVertexShader.play();
    }
    public void reset() { mVertexShader.reset(); }

    public void pause() {
        mVertexShader.pause();
    }
    public float getCurrentFrame() {return mVertexShader.getCurrentFrame(); }

    @Override
    public void bindTextures(int nextIndex) {}
    @Override
    public void unbindTextures() {}

    private final class CustomSpriteSheetVertexShaderFragment extends AShader implements IShaderFragment
    {
        public final static String SHADER_ID = "SPRITE_SHEET_VERTEX_SHADER_FRAGMENT";

        private static final String U_CURRENT_FRAME = "uCurrentFrame";
        private static final String U_NUM_TILES = "uNumTiles";

        private RFloat muCurrentFrame;
        private RVec2 muNumTiles;

        private int muCurrentFrameHandle;
        private int muNumTilesHandle;

        private float mCurrentFrame;
        private float[] mNumTiles = new float[2];

        private long mStartTime;
        private boolean mIsPlaying = false;
        private float mFPS = 30;
        private int mNumFrames;
        private boolean mIsLoop = true;

        public CustomSpriteSheetVertexShaderFragment()
        {
            super(ShaderType.VERTEX_SHADER_FRAGMENT);
            initialize();
        }

        @Override
        public void initialize()
        {
            super.initialize();

            muCurrentFrame = (RFloat) addUniform(U_CURRENT_FRAME, DataType.FLOAT);
            muNumTiles = (RVec2) addUniform(U_NUM_TILES, DataType.VEC2);
        }

        @Override
        public void setLocations(int programHandle) {
            muCurrentFrameHandle = getUniformLocation(programHandle, U_CURRENT_FRAME);
            muNumTilesHandle = getUniformLocation(programHandle, U_NUM_TILES);
        }

        @Override
        public void applyParams() {
            super.applyParams();

            if(mIsPlaying) {
                mCurrentFrame = (int) Math.floor(
                        (SystemClock.elapsedRealtime() - mStartTime) * (mFPS / 1000.f)
                ) % mNumFrames;

                if (mIsLoop == false && mCurrentFrame == (mNumFrames - 1)) {
                    pause();
                    mCurrentFrame = mNumFrames - 1;
                }
            }


            GLES20.glUniform1f(muCurrentFrameHandle, mCurrentFrame);
            GLES20.glUniform2fv(muNumTilesHandle, 1, mNumTiles, 0);
        }

        @Override
        public void main() {
            RVec2 gTextureCoord = (RVec2) getGlobal(DefaultShaderVar.G_TEXTURE_COORD);

            RFloat tileSizeX = new RFloat("tileSizeX");
            tileSizeX.assign(1.f / mNumTiles[0]);
            RFloat tileSizeY = new RFloat("tileSizeY");
            tileSizeY.assign(1.f / mNumTiles[1]);

            RFloat texSOffset = new RFloat("texSOffset", gTextureCoord.s().multiply(tileSizeX));
            RFloat texTOffset = new RFloat("texTOffset", gTextureCoord.t().multiply(tileSizeY));
            gTextureCoord.s().assign(mod(muCurrentFrame, muNumTiles.x()).multiply(tileSizeX).add(texSOffset));
            gTextureCoord.t().assign(tileSizeY.multiply(floor(muCurrentFrame.divide(muNumTiles.y()))).add(texTOffset));
        }

        public void setNumTiles(float numTilesX, float numTilesY) {
            mNumTiles[0] = numTilesX;
            mNumTiles[1] = numTilesY;
        }

        public void setFPS(float fps)
        {
            mFPS = fps;
        }

        public void setNumFrames(int numFrames) {
            mNumFrames = numFrames;
        }

        public void play() {
            mStartTime = SystemClock.elapsedRealtime();
            mIsPlaying = true;
        }

        public void reset() {
            mCurrentFrame = 0;
            mIsPlaying = false;
        }

        public void pause() {
            mIsPlaying = false;
        }

        public void setLoop(boolean isLoop) {
            mIsLoop = isLoop;
        }

        public float getCurrentFrame() {
            return mCurrentFrame;
        }

        public String getShaderId() {
            return SHADER_ID;
        }

        @Override
        public Material.PluginInsertLocation getInsertLocation() {
            return Material.PluginInsertLocation.IGNORE;
        }

        @Override
        public void bindTextures(int nextIndex) {}
        @Override
        public void unbindTextures() {}
    }
}
