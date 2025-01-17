/*
 * Copyright © 2018 Apple Inc. and the ServiceTalk project authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.servicetalk.client.api;

import io.servicetalk.concurrent.api.Publisher;
import io.servicetalk.transport.api.ExecutionStrategy;
import io.servicetalk.transport.api.ExecutionStrategyInfluencer;

import java.util.Collection;
import java.util.Collections;

/**
 * A factory for creating {@link LoadBalancer} instances.
 *
 * @param <ResolvedAddress> The type of address after resolution.
 * @param <C> The type of connection.
 */
@FunctionalInterface
public interface LoadBalancerFactory<ResolvedAddress, C extends LoadBalancedConnection> extends
            ExecutionStrategyInfluencer<ExecutionStrategy> {

    /**
     * Create a new {@link LoadBalancer}.
     *
     * @param eventPublisher A stream of {@link ServiceDiscovererEvent}s which the {@link LoadBalancer} can use to
     * connect to physical hosts. Typically generated from a
     * {@link ServiceDiscoverer#discover(Object) ServiceDiscoverer}.
     * @param connectionFactory {@link ConnectionFactory} that the returned {@link LoadBalancer} will use to generate
     * new connections. Returned {@link LoadBalancer} will own the responsibility for this {@link ConnectionFactory}
     * and hence will call {@link ConnectionFactory#closeAsync()} when {@link LoadBalancer#closeAsync()} is called.
     * @param <T> Type of connections created by the passed {@link ConnectionFactory}.
     * @return a new {@link LoadBalancer}.
     * @deprecated In the future only {@link #newLoadBalancer(String, Publisher, ConnectionFactory)} will remain,
     * please use that method instead.
     */
    @Deprecated // FIXME: 0.43 - remove deprecated method
    default <T extends C> LoadBalancer<T> newLoadBalancer(
            Publisher<? extends ServiceDiscovererEvent<ResolvedAddress>> eventPublisher,
            ConnectionFactory<ResolvedAddress, T> connectionFactory) {
        return newLoadBalancer("UNKNOWN",
                eventPublisher.map(Collections::singletonList), connectionFactory);
    }

    /**
     * Create a new {@link LoadBalancer}.
     *
     * @param targetResource A {@link String} representation of the target resource for which the created instance
     * will perform load balancing. Bear in mind, load balancing is performed over the a collection of hosts provided
     * via the {@code eventPublisher} which may not correspond directly to a single unresolved address, but potentially
     * a merged collection.
     * @param eventPublisher A stream of {@link Collection}&lt;{@link ServiceDiscovererEvent}&gt;
     * which the {@link LoadBalancer} can use to connect to physical hosts. Typically generated
     * from {@link ServiceDiscoverer#discover(Object) ServiceDiscoverer}.
     * @param connectionFactory {@link ConnectionFactory} that the returned {@link LoadBalancer} will use to generate
     * new connections. Returned {@link LoadBalancer} will own the responsibility for this {@link ConnectionFactory}
     * and hence will call {@link ConnectionFactory#closeAsync()} when {@link LoadBalancer#closeAsync()} is called.
     * @param <T> Type of connections created by the passed {@link ConnectionFactory}.
     * @return a new {@link LoadBalancer}.
     */
    <T extends C> LoadBalancer<T> newLoadBalancer(
            String targetResource,
            Publisher<? extends Collection<? extends ServiceDiscovererEvent<ResolvedAddress>>> eventPublisher,
            ConnectionFactory<ResolvedAddress, T> connectionFactory);

    @Override
    default ExecutionStrategy requiredOffloads() {
        // safe default--implementations are expected to override
        return ExecutionStrategy.offloadAll();
    }
}
