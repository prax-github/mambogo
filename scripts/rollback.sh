#!/bin/bash

# Mambogo Kubernetes Rollback Script
# Usage: ./rollback.sh <service-name> <revision> [namespace]

set -e

SERVICE_NAME=$1
REVISION=$2
NAMESPACE=${3:-mambogo-prod}

if [ -z "$SERVICE_NAME" ] || [ -z "$REVISION" ]; then
    echo "Usage: $0 <service-name> <revision> [namespace]"
    echo "Example: $0 order-service 2 mambogo-prod"
    echo "Example: $0 payment-service 1"
    exit 1
fi

echo "🔄 Rolling back ${SERVICE_NAME} to revision ${REVISION} in namespace ${NAMESPACE}..."

# Check if deployment exists
if ! kubectl get deployment ${SERVICE_NAME} -n ${NAMESPACE} >/dev/null 2>&1; then
    echo "❌ Deployment ${SERVICE_NAME} not found in namespace ${NAMESPACE}"
    exit 1
fi

# Check if revision exists
if ! kubectl rollout history deployment/${SERVICE_NAME} -n ${NAMESPACE} | grep -q "revision ${REVISION}"; then
    echo "❌ Revision ${REVISION} not found for deployment ${SERVICE_NAME}"
    echo "Available revisions:"
    kubectl rollout history deployment/${SERVICE_NAME} -n ${NAMESPACE}
    exit 1
fi

# Rollback to specific revision
echo "📦 Rolling back to revision ${REVISION}..."
kubectl rollout undo deployment/${SERVICE_NAME} --to-revision=${REVISION} -n ${NAMESPACE}

# Wait for rollback to complete
echo "⏳ Waiting for rollback to complete..."
kubectl rollout status deployment/${SERVICE_NAME} -n ${NAMESPACE}

echo "✅ Rollback completed successfully!"
echo ""
echo "📊 Current status:"
kubectl get pods -l app=${SERVICE_NAME} -n ${NAMESPACE}
echo ""
echo "📋 Rollout history:"
kubectl rollout history deployment/${SERVICE_NAME} -n ${NAMESPACE}
