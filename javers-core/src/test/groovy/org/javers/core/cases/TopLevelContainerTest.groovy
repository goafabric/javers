package org.javers.core.cases

import org.javers.core.JaversBuilder
import org.javers.core.diff.changetype.container.ArrayChange
import org.javers.core.diff.changetype.container.ListChange
import org.javers.core.diff.changetype.container.SetChange
import org.javers.core.diff.changetype.map.MapChange
import spock.lang.Specification
import spock.lang.Unroll

import static org.javers.repository.jql.UnboundedValueObjectIdDTO.*

/**
 * @author bartosz walacik
 */
class TopLevelContainerTest extends Specification {

    @Unroll
    def "should compare top-level #colType(s)"() {
        given:
        def javers = JaversBuilder.javers().build();

        when:
        def diff = javers.compare(container1, container2)

        then:
        diff.changes.size() == 1
        diff.changes[0].propertyName == colType

        where:
        colType << ["map","list","set", "objects"]
        expectedChangeType << [MapChange, ListChange, SetChange, ArrayChange]
        container1 << [ [a:1], [1], [1] as Set, (int[]) [1,2,3]]
        container2 << [ [a:1 , b:2], [1,2], [1,2] as Set, (int[]) [1,2]]
    }

    @Unroll
    def "should allow committing and querying top-level #colType(s)"() {
        given:
        def javers = JaversBuilder.javers().build();

        when:
        javers.commit("author",container1)
        javers.commit("author",container2)


        def changes = javers.getChangeHistory(voId,3)

        then:
        changes[0].propertyName == colType

        where:
        colType << ["map","list","set", "objects"]
        expectedChangeType << [MapChange, ListChange, SetChange, ArrayChange]
        container1 << [ [a:1], [1], [1] as Set, [1, 2].toArray()]
        container2 << [ [a:1 , b:2], [1,2], [1,2] as Set, [1].toArray()]
        voId << [unboundedMapId(), unboundedListId(), unboundedSetId(), unboundedArrayId()]
    }

}

