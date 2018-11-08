package org.javers.repository.sql.session;

import org.javers.common.exception.JaversException;
import org.javers.common.exception.JaversExceptionCode;
import org.javers.repository.sql.DialectName;

import static org.javers.repository.sql.session.Parameter.longParam;

class Dialects {

    static Dialect fromName(DialectName dialectName) {
        if (DialectName.H2 == dialectName) {
            return new H2(dialectName);
        }
        if (DialectName.MYSQL == dialectName) {
            return new MysqlDialect(dialectName);
        }
        if (DialectName.POSTGRES == dialectName) {
            return new PostgresDialect(dialectName);
        }
        if (DialectName.ORACLE == dialectName) {
            return new OracleDialect(dialectName);
        }
        if (DialectName.MSSQL == dialectName) {
            return new MsSqlDialect(dialectName);
        }
        throw new JaversException(JaversExceptionCode.UNSUPPORTED_SQL_DIALECT, dialectName);
    }

    static class H2 extends Dialect {
        H2(DialectName dialectName) {
            super(dialectName);
        }

        @Override
        KeyGenerator getKeyGenerator() {
            return new KeyGenerator.Sequence() {
                public String nextFromSequenceAsSelect(String seqName) {
                    return "SELECT " + nextFromSequenceEmbedded(seqName);
                }

                public String nextFromSequenceEmbedded(String seqName) {
                    return seqName + ".nextval";
                }
            };
        }
    }

    static class MysqlDialect extends Dialect {
        MysqlDialect(DialectName dialectName) {
            super(dialectName);
        }

        @Override
        KeyGenerator getKeyGenerator() {
            return (KeyGenerator.Autoincrement) () -> "select last_insert_id()";
        }
    }

    static class MsSqlDialect extends Dialect {
        MsSqlDialect(DialectName dialectName) {
            super(dialectName);
        }

        @Override
        KeyGenerator getKeyGenerator() {
            return new KeyGenerator.Sequence() {
                public String nextFromSequenceAsSelect(String seqName) {
                    return "SELECT + " + nextFromSequenceEmbedded(seqName);
                }

                public String nextFromSequenceEmbedded(String seqName) {
                    return "NEXT VALUE FOR "+seqName;
                }
            };
        }

        @Override
        void limit(SelectBuilder query, long limit, long offset) {
            if (limit == 0){
                return;
            }

            // if (offset == 0) {
            // query.wrap("select TOP "+limit.getLimit()+" * from (",") a");

            query.append("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", longParam(offset), longParam(limit));
        }
    }

    static class PostgresDialect extends Dialect {
        PostgresDialect(DialectName dialectName) {
            super(dialectName);
        }

        @Override
        KeyGenerator getKeyGenerator() {
            return new KeyGenerator.Sequence() {
                public String nextFromSequenceAsSelect(String seqName) {
                    return "SELECT "+ nextFromSequenceEmbedded(seqName);
                }

                public String nextFromSequenceEmbedded(String seqName) {
                    return "nextval('" + seqName + "')";
                }
            };
        }
    }

    static class OracleDialect extends Dialect {
        OracleDialect(DialectName dialectName) {
            super(dialectName);
        }

        @Override
        KeyGenerator getKeyGenerator() {
            return new KeyGenerator.Sequence() {
                public String nextFromSequenceAsSelect(String seqName) {
                    return "SELECT "+ nextFromSequenceEmbedded(seqName) +" from dual";
                }

                public String nextFromSequenceEmbedded(String seqName) {
                    return seqName + ".nextval";
                }
            };
        }

        @Override
        void limit(SelectBuilder query, long limit, long offset) {
            if (limit == 0) {
                return;
            }

            if (offset == 0){
                query.wrap("SELECT a.*, rownum FROM (", ") a WHERE rownum <= ?", longParam(limit));
            } else {
                long lowRownum = offset + 1;
                long highRownum = offset + limit;
                query.wrap(
                        "select b.* from (SELECT a.*, rownum r__ FROM (",
                        ") a WHERE rownum <= ? ) b where b.r__ >= ?", longParam(highRownum), longParam(lowRownum));
            }
        }
    }
}