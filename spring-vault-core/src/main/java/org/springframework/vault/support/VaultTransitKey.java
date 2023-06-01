/*
 * Copyright 2016-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.vault.support;

import java.util.Map;

/**
 * A key inside Vault's {@code transit} backend.
 *
 * @author Mark Paluch
 * @author Sven Schürmann
 */
public interface VaultTransitKey {

	/**
	 * @return name of the key
	 */
	String getName();

	/**
	 * @return the key type ({@code aes-gcm}, {@code ecdsa-p256}, ...).
	 */
	String getType();

	/**
	 * @return {@literal true} if deletion of the key is allowed. Key deletion must be
	 * turned on to make keys deletable.
	 */
	boolean isDeletionAllowed();

	/**
	 * @return {@literal true} if key derivation MUST be used.
	 */
	boolean isDerived();

	/**
	 * @return {@literal true} if the raw key is exportable.
	 */
	boolean isExportable();

	/**
	 * @return a {@link Map} of key version to its Vault-specific representation.
	 */
	Map<String, Object> getKeys();

	/**
	 * @return the latest key version.
	 */
	int getLatestVersion();

	/**
	 * @return required key version to still be able to decrypt data.
	 */
	int getMinDecryptionVersion();

	/**
	 * @return required key version to encrypt data.
	 * @since 1.1
	 */
	int getMinEncryptionVersion();

	/**
	 * @return whether the key supports decryption.
	 * @since 1.1
	 */
	boolean supportsDecryption();

	/**
	 * @return whether the key supports encryption.
	 * @since 1.1
	 */
	boolean supportsEncryption();

	/**
	 * @return whether the key supports derivation.
	 * @since 1.1
	 */
	boolean supportsDerivation();

	/**
	 * @return whether the key supports signing.
	 * @since 1.1
	 */
	boolean supportsSigning();

	/**
	 * @return if set, enables taking backup of named key in the plaintext format. Once
	 * set, this cannot be disabled.
	 */
	boolean allowPlaintextBackup();

	/**
	 * @return If enabled, the key will support convergent encryption, where the same
	 * plaintext creates the same ciphertext. This requires 'derived' to be set to true.
	 */
	boolean supportsConvergentEncryption();

	/**
	 * @return the version of the convergent nonce to use. Note: since version 3 the
	 * algorithm used in `transit`'s convergent encryption returns -1 since the version is
	 * stored with the key. For backwards compatability this field might be interesting.
	 */
	int getConvergentVersion();

}
