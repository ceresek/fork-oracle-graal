/*
 * Copyright (c) 2016, Oracle and/or its affiliates.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.truffle.llvm.nodes.memory.load;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.LLVMBoxedPrimitive;
import com.oracle.truffle.llvm.runtime.LLVMVirtualAllocationAddress;
import com.oracle.truffle.llvm.runtime.LLVMTruffleObject;
import com.oracle.truffle.llvm.runtime.global.LLVMGlobalVariable;
import com.oracle.truffle.llvm.runtime.global.LLVMGlobalVariableAccess;
import com.oracle.truffle.llvm.runtime.interop.convert.ForeignToLLVM;
import com.oracle.truffle.llvm.runtime.interop.convert.ForeignToLLVM.ForeignToLLVMType;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemory;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;

// Truffle has no branch profiles for short
@NodeChild(type = LLVMExpressionNode.class)
public abstract class LLVMI16LoadNode extends LLVMExpressionNode {
    @Child protected Node foreignRead = Message.READ.createNode();
    @Child protected ForeignToLLVM toLLVM = ForeignToLLVM.create(ForeignToLLVMType.I16);

    protected short doForeignAccess(LLVMTruffleObject addr) {
        try {
            int index = (int) (addr.getOffset() / LLVMExpressionNode.I16_SIZE_IN_BYTES);
            Object value = ForeignAccess.sendRead(foreignRead, addr.getObject(), index);
            return (short) toLLVM.executeWithTarget(value);
        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException(e);
        }
    }

    @Specialization
    public short executeShort(LLVMGlobalVariable addr, @Cached("createGlobalAccess()") LLVMGlobalVariableAccess globalAccess) {
        return globalAccess.getI16(addr);
    }

    @Specialization
    public short executeShort(LLVMAddress addr) {
        return LLVMMemory.getI16(addr);
    }

    @Specialization
    public short executeI16(LLVMVirtualAllocationAddress address) {
        return address.getI16();
    }

    @Specialization
    public short executeShort(LLVMTruffleObject addr) {
        return doForeignAccess(addr);
    }

    @Specialization
    public short executeLLVMBoxedPrimitive(LLVMBoxedPrimitive addr) {
        if (addr.getValue() instanceof Long) {
            return LLVMMemory.getI16((long) addr.getValue());
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalAccessError("Cannot access address: " + addr.getValue());
        }
    }

    @Specialization(guards = "notLLVM(addr)")
    public short executeShort(TruffleObject addr) {
        return executeShort(new LLVMTruffleObject(addr, PrimitiveType.I16));
    }

}
