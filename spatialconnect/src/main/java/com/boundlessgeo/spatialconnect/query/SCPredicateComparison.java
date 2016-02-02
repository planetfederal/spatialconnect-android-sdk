/*
 *
 *  * ****************************************************************************
 *  *  Licensed to the Apache Software Foundation (ASF) under one
 *  *  or more contributor license agreements.  See the NOTICE file
 *  *  distributed with this work for additional information
 *  *  regarding copyright ownership.  The ASF licenses this file
 *  *  to you under the Apache License, Version 2.0 (the
 *  *  "License"); you may not use this file except in compliance
 *  *  with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing,
 *  *  software distributed under the License is distributed on an
 *  *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  *  KIND, either express or implied.  See the License for the
 *  *  specific language governing permissions and limitations
 *  *  under the License.
 *  * ****************************************************************************
 *
 */

package com.boundlessgeo.spatialconnect.query;


public enum SCPredicateComparison
{
    SCPREDICATE_OPERATOR_EQUAL,
    SCPREDICATE_OPERATOR_NOTEQUAL,
    SCPREDICATE_OPERATOR_LESSTHAN,
    SCPREDICATE_OPERATOR_LESSTHAN_OREQUAL,
    SCPREDICATE_OPERATOR_GREATERTHAN,
    SCPREDICATE_OPERATOR_GREATERTHAN_OREQUAL,
    SCPREDICATE_OPERATOR_IN,
    SCPREDICATE_OPERATOR_BETWEEN,
    SCPREDICATE_OPERATOR_LIKE,
    SCPREDICATE_OPERATOR_IS_NULL,
    SCPREDICATE_OPERATOR_IS_NOT_NULL
}
