package com.sleepcompany.rfidapp.databinding;
import com.sleepcompany.rfidapp.R;
import com.sleepcompany.rfidapp.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class ItemEpcBindingImpl extends ItemEpcBinding implements com.sleepcompany.rfidapp.generated.callback.OnClickListener.Listener {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.label_map, 4);
    }
    // views
    @NonNull
    private final android.widget.LinearLayout mboundView0;
    // variables
    @Nullable
    private final android.view.View.OnClickListener mCallback1;
    // values
    // listeners
    // Inverse Binding Event Handlers

    public ItemEpcBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 5, sIncludes, sViewsWithIds));
    }
    private ItemEpcBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 0
            , (android.widget.Button) bindings[3]
            , (android.widget.TextView) bindings[4]
            , (android.widget.TextView) bindings[1]
            , (android.widget.TextView) bindings[2]
            );
        this.btnMap.setTag(null);
        this.mboundView0 = (android.widget.LinearLayout) bindings[0];
        this.mboundView0.setTag(null);
        this.tvEpc.setTag(null);
        this.tvNums.setTag(null);
        setRootTag(root);
        // listeners
        mCallback1 = new com.sleepcompany.rfidapp.generated.callback.OnClickListener(this, 1);
        invalidateAll();
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
                mDirtyFlags = 0x4L;
        }
        requestRebind();
    }

    @Override
    public boolean hasPendingBindings() {
        synchronized(this) {
            if (mDirtyFlags != 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean setVariable(int variableId, @Nullable Object variable)  {
        boolean variableSet = true;
        if (BR.handler == variableId) {
            setHandler((com.sleepcompany.rfidapp.InventoryFragment.HandlerClick) variable);
        }
        else if (BR.epc == variableId) {
            setEpc((com.seuic.uhf.EPC) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setHandler(@Nullable com.sleepcompany.rfidapp.InventoryFragment.HandlerClick Handler) {
        this.mHandler = Handler;
        synchronized(this) {
            mDirtyFlags |= 0x1L;
        }
        notifyPropertyChanged(BR.handler);
        super.requestRebind();
    }
    public void setEpc(@Nullable com.seuic.uhf.EPC Epc) {
        this.mEpc = Epc;
        synchronized(this) {
            mDirtyFlags |= 0x2L;
        }
        notifyPropertyChanged(BR.epc);
        super.requestRebind();
    }

    @Override
    protected boolean onFieldChange(int localFieldId, Object object, int fieldId) {
        switch (localFieldId) {
        }
        return false;
    }

    @Override
    protected void executeBindings() {
        long dirtyFlags = 0;
        synchronized(this) {
            dirtyFlags = mDirtyFlags;
            mDirtyFlags = 0;
        }
        com.sleepcompany.rfidapp.InventoryFragment.HandlerClick handler = mHandler;
        java.lang.String stringValueOfEpcCount = null;
        int epcCount = 0;
        java.lang.String epcGetId = null;
        com.seuic.uhf.EPC epc = mEpc;

        if ((dirtyFlags & 0x6L) != 0) {



                if (epc != null) {
                    // read epc.count
                    epcCount = epc.count;
                    // read epc.getId()
                    epcGetId = epc.getId();
                }


                // read String.valueOf(epc.count)
                stringValueOfEpcCount = java.lang.String.valueOf(epcCount);
        }
        // batch finished
        if ((dirtyFlags & 0x4L) != 0) {
            // api target 1

            this.btnMap.setOnClickListener(mCallback1);
        }
        if ((dirtyFlags & 0x6L) != 0) {
            // api target 1

            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.tvEpc, epcGetId);
            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.tvNums, stringValueOfEpcCount);
        }
    }
    // Listener Stub Implementations
    // callback impls
    public final void _internalCallbackOnClick(int sourceId , android.view.View callbackArg_0) {
        // localize variables for thread safety
        // epc.id
        java.lang.String epcId = null;
        // handler
        com.sleepcompany.rfidapp.InventoryFragment.HandlerClick handler = mHandler;
        // handler.onTagClick(epc.id)
        com.sleepcompany.rfidapp.InventoryFragment.HandlerClick handlerOnTagClickEpcId = null;
        // handler != null
        boolean handlerJavaLangObjectNull = false;
        // epc != null
        boolean epcJavaLangObjectNull = false;
        // epc
        com.seuic.uhf.EPC epc = mEpc;



        handlerJavaLangObjectNull = (handler) != (null);
        if (handlerJavaLangObjectNull) {



            epcJavaLangObjectNull = (epc) != (null);
            if (epcJavaLangObjectNull) {


                epcId = epc.getId();

                handlerOnTagClickEpcId = handler.onTagClick(epcId);
            }
        }
    }
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): handler
        flag 1 (0x2L): epc
        flag 2 (0x3L): null
    flag mapping end*/
    //end
}